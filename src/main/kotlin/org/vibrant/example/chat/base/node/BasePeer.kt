package org.vibrant.example.chat.base.node

import kotlinx.coroutines.experimental.*
import mu.KotlinLogging
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse
import org.vibrant.core.node.AbstractPeer
import org.vibrant.core.node.RemoteNode
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.coroutines.experimental.suspendCoroutine

class BasePeer(port: Int, private val node: BaseNode): AbstractPeer<RemoteNode>(port) {

    protected val logger = KotlinLogging.logger {}
    /**
     * Socket to receive packages
     */
    private val socket = DatagramSocket(this.port)



    private var serverSession: Thread? = null

    internal val sessions = hashMapOf<Long, BaseSession>()


    internal val allPeers = arrayListOf<RemoteNode>()
    internal val miners = arrayListOf<RemoteNode>()

    internal val onRequestReceivedListeners = arrayListOf<(JSONRPCRequest) -> Unit>()
    internal val onResponseReceivedListeners = arrayListOf<(JSONRPCResponse<*>) -> Unit>()


    override fun start() {
        this.serverSession = thread(true, name = "Request loop on port $port"){
            while(true){
                try{
                    val buf = ByteArray(65536)
                    val packet = DatagramPacket(buf, buf.size)
                    logger.info { "Waiting JSON RPC 2.0 request... $port" }
                    socket.receive(packet)
                    this@BasePeer.addUniqueRemoteNode(RemoteNode(packet.address.hostName, packet.port))
                    //idk why coroutine ain't working here.
                    async {
                        this@BasePeer.handlePackage(buf, packet)
                    }
                    logger.info { "Suspended handler $port" }
                }catch (e: SocketException){
                    break
                }
            }
        }

    }

    private suspend fun handlePackage(buf: ByteArray, packet: DatagramPacket){
        val entity = BaseJSONSerializer().deserializeJSONRPC(String(buf))
        when (entity) {
            is JSONRPCRequest -> {
                logger.info { "Received request $entity" }
                val response = node.rpc.invoke(entity, RemoteNode(packet.address.hostName, packet.port))
                logger.info { "Responding with $response" }
                val responseBytes = BaseJSONSerializer().serialize(response).toByteArray()
                socket.send(DatagramPacket(responseBytes, responseBytes.size, packet.socketAddress))
                this@BasePeer.node.possibleAheads.clear()
                this@BasePeer.onRequestReceivedListeners.forEach { it(entity) }
            }
            is JSONRPCResponse<*> -> {
                logger.info { "Received response $entity, handling..." }
                this@BasePeer.sessions[entity.id]?.handle(entity)
                this@BasePeer.onResponseReceivedListeners.forEach { it(entity) }
            }
            else -> {
                logger.info { "Received something really crazy $entity" }
            }
        }
    }

    internal fun addUniqueRemoteNode(remoteNode: RemoteNode, miner: Boolean = false) {
        if (this.allPeers.find { it.address == remoteNode.address && it.port == remoteNode.port } == null) {
            this.allPeers.add(remoteNode)
        }
        if (miner && this.miners.find { it.address == remoteNode.address && it.port == remoteNode.port } == null) {
            this.miners.add(remoteNode)
        }
    }


    suspend fun broadcast(request: JSONRPCRequest): List<JSONRPCResponse<*>> {
        return this.allPeers.map {
            this@BasePeer.send(it, request)
        }
    }

    suspend fun send(remoteNode: RemoteNode, request: JSONRPCRequest): JSONRPCResponse<*> {
        val serialized = BaseJSONSerializer().serialize(request).toByteArray()
        val latch = CountDownLatch(1)
        var deferredResponse: JSONRPCResponse<*>? = null
        this@BasePeer.sessions[request.id] = object : BaseSession(remoteNode, request){
            override fun handle(response: JSONRPCResponse<*>) {
                deferredResponse = response
                this@BasePeer.sessions.remove(request.id)
                latch.countDown()
            }
        }

        socket.send(DatagramPacket(serialized, serialized.size, InetSocketAddress(remoteNode.address, remoteNode.port)))
        latch.await()
        return deferredResponse!!
    }


    override fun stop() {
        this.socket.close()
        this.socket.disconnect()
        this.serverSession?.interrupt()
    }

}