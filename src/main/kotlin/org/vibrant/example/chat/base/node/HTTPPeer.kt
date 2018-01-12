package org.vibrant.example.chat.base.node

import io.javalin.Javalin
import mu.KotlinLogging
import org.vibrant.core.node.AbstractPeer
import org.vibrant.core.node.RemoteNode
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result

class HTTPPeer(val node: BaseNode, port: Int): AbstractPeer<RemoteNode>(port){

    private val logger = KotlinLogging.logger{}

    private val server = createServer()


    val miners: ArrayList<RemoteNode> = arrayListOf()
    val peers: ArrayList<RemoteNode> = arrayListOf()


    init {
        logger.info { "I AM PEER ON PORT $port" }
    }

    private fun createServer(): Javalin {
        return Javalin
                .create()
                .before{ ctx ->
                    val remotePort = ctx.header("peer-port")?.toInt()
                    if(remotePort != null){
                        logger.info { "Got request from  ${ctx.request().remoteAddr}:${remotePort}" }
                        this@HTTPPeer.addUniqueRemoteNode(RemoteNode(ctx.request().remoteAddr, remotePort))
                    }
                }
                .post("rpc", { ctx ->
                    val remotePort = ctx.header("peer-port")?.toInt()
                    if (remotePort != null) {
                        val remoteNode = RemoteNode(ctx.request().remoteAddr, remotePort)
                        val jsonRPCRequest = BaseJSONSerializer.deserializeJSONRPC(ctx.body()) as JSONRPCRequest
                        logger.info { "Received request $jsonRPCRequest" }
                        val response = node.rpc.invoke(jsonRPCRequest, remoteNode)
                        logger.info { "Responding with $response" }
                        ctx.result(BaseJSONSerializer.serialize(response))
                    }else{
                        ctx.status(400).result("Expected header 'peer-port'")
                    }
                })
                .port(port)
    }


    fun request(remoteNode: RemoteNode, jsonrpcRequest: JSONRPCRequest): JSONRPCResponse<*>{
        val(_, _, result) =  "http://${remoteNode.address}:${remoteNode.port}/rpc"
                .httpPost()
                .header("peer-port" to port)
                .body(BaseJSONSerializer.serialize(jsonrpcRequest))
                .responseString()
        return BaseJSONSerializer.deserializeJSONRPC(result.get()) as JSONRPCResponse<*>
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


    fun addUniqueRemoteNode(remoteNode: RemoteNode, miner: Boolean = false) {
        if (this.peers.find { it.address == remoteNode.address && it.port == remoteNode.port } == null) {
            this.peers.add(remoteNode)
        }
        if (miner && this.miners.find { it.address == remoteNode.address && it.port == remoteNode.port } == null) {
            this.miners.add(remoteNode)
        }
    }
}