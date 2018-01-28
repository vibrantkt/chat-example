@file:Suppress("UNUSED_PARAMETER")

package org.vibrant.example.chat.base.node

import kotlinx.coroutines.experimental.runBlocking
import mu.KLogger
import mu.KotlinLogging
import org.vibrant.base.database.blockchain.BlockChain
import org.vibrant.base.database.blockchain.InMemoryBlockChain
import org.vibrant.base.database.blockchain.InstantiateBlockChain
import org.vibrant.base.rpc.json.JSONRPC
import org.vibrant.base.rpc.json.JSONRPCBlockChainSynchronization
import org.vibrant.base.rpc.json.JSONRPCRequest
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.core.node.RemoteNode
import org.vibrant.base.rpc.json.JSONRPCResponse
import org.vibrant.core.ConcreteModelSerializer
import org.vibrant.example.chat.base.models.BaseBlockChainModel
import org.vibrant.example.chat.base.producers.BaseBlockChainProducer

import org.vibrant.example.chat.base.util.serialize
import org.vibrant.example.chat.base.util.serializerFor

open class BaseJSONRPCProtocol(override val node: Node): JSONRPC(),
        JSONRPCBlockChainSynchronization<Peer, BaseBlockModel, BaseTransactionModel, BaseBlockChainModel> {

    override val blockSerializer: ConcreteModelSerializer<BaseBlockModel> = serializerFor()

    override val broadcastedBlocks: ArrayList<String> = arrayListOf()

    override val broadcastedTransactions: ArrayList<String> = arrayListOf()

    override val chain: InMemoryBlockChain<BaseBlockModel, BaseBlockChainModel>
        get() = node.chain

    override val chainSerializer: ConcreteModelSerializer<BaseBlockChainModel> = serializerFor()

    override val logger: KLogger = KotlinLogging.logger{}

    override val modelToProducer: InstantiateBlockChain<BaseBlockModel, BaseBlockChainModel> = object: InstantiateBlockChain<BaseBlockModel, BaseBlockChainModel>{
            override fun asBlockChainProducer(model: BaseBlockChainModel): BlockChain<BaseBlockModel, BaseBlockChainModel> {
                val producer = BaseBlockChainProducer()
                producer.blocks().clear()
                producer.blocks().addAll(model.blocks)
                return producer
            }

        }

    override val transactionSerializer: ConcreteModelSerializer<BaseTransactionModel> = serializerFor()

    override fun handleDistinctTransaction(transaction: BaseTransactionModel) {
        if(node is BaseMiner){
            (node as BaseMiner).addTransaction(transaction)
        }
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