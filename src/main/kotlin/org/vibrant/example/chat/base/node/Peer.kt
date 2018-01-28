package org.vibrant.example.chat.base.node


import org.vibrant.base.http.HTTPJsonRPCPeer
import org.vibrant.base.rpc.json.JSONRPCRequest
import org.vibrant.core.node.RemoteNode
import org.vibrant.base.rpc.json.JSONRPCResponse


class Peer(port: Int, rpc: BaseJSONRPCProtocol): HTTPJsonRPCPeer(port, rpc){

    val miners = arrayListOf<RemoteNode>()


    fun broadcastMiners(jsonrpcRequest: JSONRPCRequest): List<JSONRPCResponse<*>> {
        return this.broadcast(jsonrpcRequest, this.miners)
    }


    fun addUniqueRemoteNode(remoteNode: RemoteNode, isMiner: Boolean = false) {
        super.addUniqueRemoteNode(remoteNode)
        if (isMiner && this.miners.find { it.address == remoteNode.address && it.port == remoteNode.port } == null) {
            this.miners.add(remoteNode)
        }
    }
}