@file:Suppress("UNUSED_PARAMETER")

package org.vibrant.example.chat.base.node

import kotlinx.coroutines.experimental.runBlocking
import org.vibrant.base.rpc.json.JSONRPC
import org.vibrant.base.rpc.json.JSONRPCRequest
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.core.node.RemoteNode
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse
import org.vibrant.example.chat.base.util.serialize

open class BaseJSONRPCProtocol(val node: Node): JSONRPC() {

    @JSONRPCMethod
    fun addTransaction(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*> {
        val transaction = BaseJSONSerializer.deserialize(request.params[0].toString().toByteArray()) as BaseTransactionModel
        if(node is BaseMiner){
            runBlocking {
                node.addTransaction(transaction)
            }
        }
        logger.info { "Returning: Block mined!" }
        return JSONRPCResponse(
                result = node is BaseMiner,
                error = null,
                id = request.id
        )
    }


    @JSONRPCMethod
    fun getLastBlock(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*>{
        return JSONRPCResponse(
                result = node.chain.latestBlock().serialize(),
                error = null,
                id = request.id
        )
    }


    @JSONRPCMethod
    fun newBlock(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*>{

        val blockModel = BaseJSONSerializer.deserialize(request.params[0].toString().toByteArray()) as BaseBlockModel
        node.handleLastBlock(blockModel, remoteNode)
        return JSONRPCResponse(
                result = node.chain.latestBlock().serialize(),
                error = null,
                id = request.id
        )
    }


    @JSONRPCMethod
    fun syncWithMe(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*>{
        logger.info { "${node.peer.port} Requested sync, starting... " }
        node.synchronize(remoteNode)
        logger.info { "Sync finished, responding..." }
        return JSONRPCResponse(
                result = true,
                error = null,
                id = request.id
        )
    }


    @JSONRPCMethod
    fun getFullChain(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*>{
        return JSONRPCResponse(
                result = node.chain.produce(BaseJSONSerializer).serialize(),
                error = null,
                id = request.id
        )
    }


    @Suppress("USELESS_IS_CHECK")
    @JSONRPCMethod
    fun nodeType(request: JSONRPCRequest, remoteNode: RemoteNode): JSONRPCResponse<*> {
        node.peer.addUniqueRemoteNode(remoteNode, request.params[0] == "miner")
        return JSONRPCResponse(
                result = when(node){
                    is BaseMiner -> "miner"
                    is Node -> "node"
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
}