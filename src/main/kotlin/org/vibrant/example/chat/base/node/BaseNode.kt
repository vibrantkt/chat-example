package org.vibrant.example.chat.base.node

import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.models.BaseBlockChainModel
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.example.chat.base.producers.BaseBlockChainProducer
import org.vibrant.core.node.AbstractNode
import org.vibrant.core.node.RemoteNode
import org.vibrant.core.producers.BlockChainProducer

open class BaseNode(private val port: Int) : AbstractNode<BaseBlockChainModel, BaseBlockChainProducer>() {
    protected var requestID = 0L
    protected val logger = KotlinLogging.logger {  }

    @Suppress("LeakingThis") internal val rpc = BaseJSONRPCProtocol(this)
    internal val peer = BasePeer(port, this)

    internal val possibleAheads = arrayListOf<RemoteNode>()

    override fun start() {
        this.peer.start()
    }

    override fun stop() {
        this.peer.stop()
    }

    fun handleLastBlock(lastBlock: BaseBlockModel, remoteNode: RemoteNode){
        runBlocking {
            val latestBlock = this@BaseNode.chain.latestBlock()
            if(latestBlock != lastBlock){
                logger.info { "My chain is not in sync with peer $remoteNode" }
                when {
                    lastBlock.index > latestBlock.index -> {
                        logger.info { "My chain is behind, requesting full chain" }
                        val chainResponse = this@BaseNode.peer.send(remoteNode, JSONRPCRequest("getFullChain", arrayOf(), requestID++))
                        val model = BaseJSONSerializer().deserialize(chainResponse.result.toString()) as BaseBlockChainModel
                        val tmpChain = BaseBlockChainProducer.instantiate(
                                model
                        )

                        val chainOK = tmpChain.checkIntegrity()
                        if(chainOK){
                            logger.info { "Received chain is fine, replacing" }
                            this@BaseNode.chain.dump(model)
                            logger.info { "Received chain is fine, replaced" }
                        }else{
                            logger.info { "Received chain is not fine, I ignore it" }
                        }
                    }
                    lastBlock.index == latestBlock.index -> {
                        logger.info { "My chain is same. Just leave it, i guess" }
                    }
                    else -> {
                        logger.info { "Wow i request sync with me" }
                        val response = this@BaseNode.peer.send(remoteNode, JSONRPCRequest("syncWithMe", arrayOf(), requestID++))
                    }
                }
            }else{
                logger.info { "Chain in sync with peer $remoteNode" }
            }
        }
    }


    fun synchronize(remoteNode: RemoteNode){
        runBlocking {
            logger.info { "Requesting and waiting for response get last block" }
            val response = this@BaseNode.peer.send(remoteNode, JSONRPCRequest("getLastBlock", arrayOf(), requestID++))
            val lastBlock = BaseJSONSerializer().deserialize(response.result.toString()) as BaseBlockModel
            this@BaseNode.handleLastBlock(lastBlock, remoteNode)
        }
    }

    val chain: BaseBlockChainProducer = BaseBlockChainProducer()


    override fun connect(remoteNode: RemoteNode): Boolean {
        return runBlocking {
            val response1 = this@BaseNode.peer.send(remoteNode, JSONRPCRequest("echo", arrayOf("peer"), this@BaseNode.requestID++))
            val response2 = this@BaseNode.peer.send(remoteNode, JSONRPCRequest("nodeType", arrayOf(), this@BaseNode.requestID++))
            return@runBlocking if(response1.result == "peer"){
                this@BaseNode.peer.addUniqueRemoteNode(remoteNode, response2.result.toString() == "miner")
                true
            }else{
                false
            }
        }
    }
}