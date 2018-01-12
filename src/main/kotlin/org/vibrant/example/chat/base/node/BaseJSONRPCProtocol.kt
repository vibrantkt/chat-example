@file:Suppress("UNUSED_PARAMETER")

package org.vibrant.example.chat.base.node

import kotlinx.coroutines.experimental.async
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.core.node.RemoteNode

open class BaseJSONRPCProtocol(val node: BaseNode) {


    @JSONRPCMethod
    fun addTransaction(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*>{
        val transaction = BaseJSONSerializer.deserialize(request.params[0].toString()) as BaseTransactionModel
        if(node is BaseMiner){
            node.addTransaction(transaction)
        }
        return JSONRPCResponse(
                result = node is BaseMiner,
                error = null,
                id = request.id
        )
    }


    @JSONRPCMethod
    fun getLastBlock(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*>{
        return JSONRPCResponse(
                result = BaseJSONSerializer.serialize(node.chain.latestBlock()),
                error = null,
                id = request.id
        )
    }


    @JSONRPCMethod
    fun newBlock(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*>{
        val blockModel = BaseJSONSerializer.deserialize(request.params[0] as String) as BaseBlockModel
        node.handleLastBlock(blockModel, remoteNode)
        return JSONRPCResponse(
                result = BaseJSONSerializer.serialize(node.chain.latestBlock()),
                error = null,
                id = request.id
        )
    }


    @JSONRPCMethod
    fun syncWithMe(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*>{
//        node.possibleAheads.add(remoteNode)
        node.synchronize(remoteNode)
        return JSONRPCResponse(
                result = true,
                error = null,
                id = request.id
        )
    }


    @JSONRPCMethod
    fun getFullChain(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*>{
        return JSONRPCResponse(
                result = BaseJSONSerializer.serialize(node.chain.produce(BaseJSONSerializer)),
                error = null,
                id = request.id
        )
    }


    @Suppress("USELESS_IS_CHECK")
    @JSONRPCMethod
    fun nodeType(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*> {
        return JSONRPCResponse(
                result = when(node){
                    is BaseMiner -> "miner"
                    is BaseNode -> "node"
                    else -> "node"
                },
                error = null,
                id = request.id
        )
    }


    @JSONRPCMethod
    fun echo(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*> {
        return JSONRPCResponse(
                result = request.params[0],
                error = null,
                id = request.id
        )
    }

    fun invoke(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*> {
        return this::class.java.getMethod(request.method, JSONRPCRequest::class.java, RemoteNode::class.java).invoke(this, request, remoteNode) as JSONRPCResponse<*>
    }
}