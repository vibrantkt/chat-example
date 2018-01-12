package org.vibrant.example.chat.base.node

import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import org.vibrant.core.algorithm.SignatureProducer
import org.vibrant.core.node.AbstractNode
import org.vibrant.core.node.RemoteNode
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse
import org.vibrant.example.chat.base.models.BaseBlockChainModel
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseMessageModel
import org.vibrant.example.chat.base.models.TransactionPayload
import org.vibrant.example.chat.base.producers.BaseBlockChainProducer
import org.vibrant.example.chat.base.producers.BaseTransactionProducer
import org.vibrant.example.chat.base.util.HashUtils
import java.security.KeyPair
import java.util.*

open class BaseNode(port: Int) : AbstractNode<BaseBlockChainModel, BaseBlockChainProducer>() {
    protected var requestID = 0L
    protected val logger = KotlinLogging.logger {  }

    @Suppress("LeakingThis") internal val rpc = BaseJSONRPCProtocol(this)
    internal val peer = Peer(port, this)

    internal val possibleAheads = arrayListOf<RemoteNode>()


    val onNextBlock = arrayListOf<(BaseBlockModel) -> Unit>()


    var keyPair: KeyPair? = null

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
                    lastBlock.index - latestBlock.index == 1L  && lastBlock.prevHash == latestBlock.hash -> {
                        this@BaseNode.chain.pushBlock(
                                lastBlock
                        )
                        this@BaseNode.onNextBlock.forEach { it(lastBlock) }
                        logger.info { "I just got next block. Chain good: ${chain.checkIntegrity()}" }
                    }
                    lastBlock.index > latestBlock.index -> {
                        logger.info { "My chain is behind, requesting full chain" }
                        val chainResponse = this@BaseNode.peer.request(remoteNode, JSONRPCRequest("getFullChain", arrayOf(), requestID++))  as JSONRPCResponse<*>
                        val model = BaseJSONSerializer.deserialize(chainResponse.result.toString()) as BaseBlockChainModel
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
                        this@BaseNode.peer.send(remoteNode, JSONRPCRequest("syncWithMe", arrayOf(), requestID++))
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
            val response = this@BaseNode.peer.request(remoteNode, JSONRPCRequest("getLastBlock", arrayOf(), requestID++)) as JSONRPCResponse<*>
            val lastBlock = BaseJSONSerializer.deserialize(response.result.toString()) as BaseBlockModel
            this@BaseNode.handleLastBlock(lastBlock, remoteNode)
        }
    }

    val chain: BaseBlockChainProducer = BaseBlockChainProducer(difficulty = 2)


    suspend fun transaction(to: String, payload: String): List<JSONRPCResponse<*>> {
        val transaction = BaseTransactionProducer(
                HashUtils.bytesToHex(this@BaseNode.keyPair!!.public.encoded),
                to,
                BaseMessageModel(payload, Date().time),
                this@BaseNode.keyPair!!,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)

        return this.peer.broadcastMiners(JSONRPCRequest(
                method = "addTransaction",
                params = arrayOf(BaseJSONSerializer.serialize(transaction)),
                id = this.requestID++
        ))
    }


    suspend fun transaction(to: String, payload: TransactionPayload): List<JSONRPCResponse<*>>{
        val transaction = BaseTransactionProducer(
                HashUtils.bytesToHex(this@BaseNode.keyPair!!.public.encoded),
                to,
                payload,
                this@BaseNode.keyPair!!,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)

        return this.peer.broadcastMiners(JSONRPCRequest(
                method = "addTransaction",
                params = arrayOf(BaseJSONSerializer.serialize(transaction)),
                id = this.requestID++
        ))
    }

    override fun connect(remoteNode: RemoteNode): Boolean {
        return runBlocking {
//            val response1 = this@BaseNode.peer.request(remoteNode, JSONRPCRequest("echo", arrayOf("peer"), this@BaseNode.requestID++))
//            println(response1)
            val response1 = this@BaseNode.peer.request(remoteNode, JSONRPCRequest("echo", arrayOf("peer"), this@BaseNode.requestID++)) as JSONRPCResponse<*>
            val response2 = this@BaseNode.peer.request(remoteNode, JSONRPCRequest("nodeType", arrayOf(), this@BaseNode.requestID++)) as JSONRPCResponse<*>
            return@runBlocking if(response1.result == "peer"){
                this@BaseNode.peer.addUniqueRemoteNode(remoteNode, response2.result.toString() == "miner")
                true
            }else{
                false
            }
        }
    }
}