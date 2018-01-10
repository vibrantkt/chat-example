package org.vibrant.example.chat.base.node

import org.vibrant.core.node.RemoteNode
import org.vibrant.core.node.UDPSessionPeer
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCEntity
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse
import java.net.DatagramPacket

class Peer(port: Int, val node: BaseNode) : UDPSessionPeer<JSONRPCEntity>(port, object : UDPSessionPeer.Communication.CommunicationPackageDeserializer<JSONRPCEntity>(){
    override fun fromByteArray(byteArray: ByteArray): JSONRPCEntity {
        return BaseJSONSerializer().deserializeJSONRPC(String(byteArray, charset("UTF-8")))
    }

}) {


    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    suspend override fun handlePackage(entity: JSONRPCEntity, peer: UDPSessionPeer<JSONRPCEntity>, remoteNode: RemoteNode) {
        logger.info { "Received package ${entity}" }
        when (entity) {
            is JSONRPCRequest -> {
                logger.info { "Received request $entity" }
                val response = node.rpc.invoke(entity, remoteNode)
                logger.info { "Responding with $response" }
                this.send(remoteNode, response)
                this.node.possibleAheads.clear()
            }
            is JSONRPCResponse<*> -> {
                logger.info { "Received response $entity, handling..." }
                this.sessions[entity.id]?.handle(entity)
            }
            else -> {
                logger.info { "Received something really crazy $entity" }
            }
        }
    }

    private suspend fun broadcast(jsonrpcRequest: JSONRPCRequest, peers: List<RemoteNode>): List<JSONRPCResponse<*>> {
        return peers.map {
            this.request(it, jsonrpcRequest) as JSONRPCResponse<*>
        }
    }

    suspend fun broadcastAll(jsonrpcRequest: JSONRPCRequest): List<JSONRPCResponse<*>> {
        return this.broadcast(jsonrpcRequest, this.peers)
    }

    suspend fun broadcastMiners(jsonrpcRequest: JSONRPCRequest): List<JSONRPCResponse<*>> {
        return this.broadcast(jsonrpcRequest, this.miners)
    }


//    fun broadcast()

}