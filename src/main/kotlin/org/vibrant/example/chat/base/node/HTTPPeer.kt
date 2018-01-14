package org.vibrant.example.chat.base.node

import com.github.kittinunf.fuel.httpPost
import io.javalin.Javalin
import mu.KotlinLogging
import org.vibrant.core.node.AbstractPeer
import org.vibrant.core.node.RemoteNode
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.VibrantChat
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse
import java.io.ByteArrayInputStream

class HTTPPeer(val port: Int, val handler: (ByteArray, RemoteNode) -> ByteArray): AbstractPeer(){


    private val logger = KotlinLogging.logger{}

    private val server = createServer()


    val miners = arrayListOf<RemoteNode>()
    val peers = arrayListOf<RemoteNode>()


    init {
        logger.info { "I AM PEER ON PORT $port" }
    }

    private fun createServer(): Javalin {
        return Javalin
                .create()
                .before{ ctx ->
                    val remotePort = ctx.header("peer-port")?.toInt()
                    if(remotePort != null){
                        logger.info { "Got request from  ${ctx.request().remoteAddr}:$remotePort" }
                        this@HTTPPeer.addUniqueRemoteNode(RemoteNode(ctx.request().remoteAddr, remotePort), false)
                    }
                }
                .post("rpc", { ctx ->
                    val remotePort = ctx.header("peer-port")?.toInt()
                    if (remotePort != null) {
                        val remoteNode = RemoteNode(ctx.request().remoteAddr, remotePort)
                        val jsonRPCRequest = BaseJSONSerializer.deserializeJSONRPC(ctx.body()) as JSONRPCRequest
                        logger.info { "Received request $jsonRPCRequest" }
//                        val response = this@HTTPPeer.handleData(ctx.bodyAsBytes(), remoteNode)
                        val response = this@HTTPPeer.handler(ctx.bodyAsBytes(), remoteNode)
                        logger.info { "Responding with ${String(response)}" }
                        ctx.result(ByteArrayInputStream(response))
                    }else{
                        ctx.status(400).result("Expected header 'peer-port'")
                    }
                })
                .port(port)
    }


    fun request(remoteNode: RemoteNode, jsonrpcRequest: JSONRPCRequest): JSONRPCResponse<*>{
        val response = this.request(BaseJSONSerializer.serialize(jsonrpcRequest), remoteNode)
        return BaseJSONSerializer.deserializeJSONRPC(response) as JSONRPCResponse<*>
    }

    private fun broadcast(jsonrpcRequest: JSONRPCRequest, peers: List<RemoteNode>): List<JSONRPCResponse<*>> {
        return peers.map {
            this.request(it, jsonrpcRequest)
        }
    }

    fun broadcastAll(jsonrpcRequest: JSONRPCRequest): List<JSONRPCResponse<*>> {
        return this.broadcast(jsonrpcRequest, this.peers)
    }

    fun broadcastMiners(jsonrpcRequest: JSONRPCRequest): List<JSONRPCResponse<*>> {
        return this.broadcast(jsonrpcRequest, this.miners)
    }

    override fun start() {
        server.start()
    }

    override fun stop() {
        server.stop()
    }


    override fun request(byteArray: ByteArray, remoteNode: RemoteNode): ByteArray {
        val(_, _, result) =  "http://${remoteNode.address}:${remoteNode.port}/rpc"
                .httpPost()
                .header("peer-port" to port)
                .body(String(byteArray))
                .response()

        return result.get()
    }

    fun addUniqueRemoteNode(remoteNode: RemoteNode, miner: Boolean) {
        if (this.peers.find { it.address == remoteNode.address && it.port == remoteNode.port } == null) {
            this.peers.add(remoteNode)
        }
        if (miner && this.miners.find { it.address == remoteNode.address && it.port == remoteNode.port } == null) {
            this.miners.add(remoteNode)
        }
    }
}