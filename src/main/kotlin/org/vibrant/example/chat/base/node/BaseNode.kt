package org.vibrant.example.chat.base.node

import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import org.vibrant.core.ModelSerializer
import org.vibrant.core.algorithm.SignatureProducer
import org.vibrant.core.models.Model
import org.vibrant.core.node.AbstractNode
import org.vibrant.core.node.RemoteNode
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.VibrantChat
import org.vibrant.example.chat.base.jsonrpc.JSONRPCEntity
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

@Suppress("USELESS_IS_CHECK")
open class BaseNode(port: Int) : AbstractNode<JSONRPCEntity>() {

    val peer = HTTPPeer(port, { bytes, remoteNode ->
        this.handle(BaseJSONSerializer.deserializeJSONRPC(bytes), remoteNode)
    })


    override fun request(payload: JSONRPCEntity, remoteNode: RemoteNode): JSONRPCEntity {
        return peer.request(remoteNode, payload as JSONRPCRequest)
    }


    protected var requestID = 0L
    protected val logger = KotlinLogging.logger {  }

    @Suppress("LeakingThis") internal val rpc = BaseJSONRPCProtocol(this)

    internal val possibleAheads = arrayListOf<RemoteNode>()


    val onNextBlock = arrayListOf<(BaseBlockModel) -> Unit>()


    override fun start() {
        this.peer.start()
    }

    override fun stop() {
        this.peer.stop()
    }

    fun handle(data: Model, from: RemoteNode): ByteArray {
        val response = this.rpc.invoke(data as JSONRPCRequest, from)
        return BaseJSONSerializer.serialize(response)
    }

    fun handleLastBlock(lastBlock: BaseBlockModel, remoteNode: RemoteNode){
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


                    val chainResponse = this@BaseNode.request(
                            JSONRPCRequest("getFullChain", arrayOf(), requestID++),
                            remoteNode
                    ) as JSONRPCResponse<*>

                    val model = BaseJSONSerializer.deserialize(chainResponse.result.toString().toByteArray()) as BaseBlockChainModel
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
                    val response = this@BaseNode.request(JSONRPCRequest("syncWithMe", arrayOf(), requestID++), remoteNode)
                    logger.info { "Got response! $response" }
                }
            }
        }else{
            logger.info { "Chain in sync with peer $remoteNode" }
        }
    }


    fun synchronize(remoteNode: RemoteNode){
        logger.info { "Requesting and waiting for response get last block" }
        val response = this@BaseNode.request(JSONRPCRequest("getLastBlock", arrayOf(), requestID++), remoteNode) as JSONRPCResponse<*>
        logger.info { "Got last block ${response.result}" }

        val lastBlock = BaseJSONSerializer.deserialize(response.result.toString().toByteArray()) as BaseBlockModel
        logger.info { "Last block is $lastBlock" }
        this@BaseNode.handleLastBlock(lastBlock, remoteNode)
        logger.info { "Handled" }
    }

    val chain: BaseBlockChainProducer = BaseBlockChainProducer(difficulty = 2)


    fun transaction(to: String, payload: String, keyPair: KeyPair): List<JSONRPCResponse<*>> {
        val transaction = BaseTransactionProducer(
                HashUtils.bytesToHex(keyPair.public.encoded),
                to,
                BaseMessageModel(payload, Date().time),
                keyPair,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)

        return this.broadcastMiners(JSONRPCRequest(
                method = "addTransaction",
                params = arrayOf(String(BaseJSONSerializer.serialize(transaction))),
                id = this.requestID++
        ))
    }


    fun transaction(to: String, payload: TransactionPayload, keyPair: KeyPair): List<JSONRPCResponse<*>>{
        val transaction = BaseTransactionProducer(
                HashUtils.bytesToHex(keyPair.public.encoded),
                to,
                payload,
                keyPair,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)

        return this.broadcastMiners(JSONRPCRequest(
                method = "addTransaction",
                params = arrayOf(String(BaseJSONSerializer.serialize(transaction))),
                id = this.requestID++
        ))//.map { it }
    }

    override fun connect(remoteNode: RemoteNode): Boolean {
        val response1 = this@BaseNode.request(JSONRPCRequest("echo", arrayOf("peer"), this@BaseNode.requestID++), remoteNode) as JSONRPCResponse<*>
        val response2 = this@BaseNode.request(JSONRPCRequest("nodeType", arrayOf(when(this){
            is BaseMiner -> "miner"
            is BaseNode -> "node"
            else -> "node"
        }), this@BaseNode.requestID++), remoteNode) as JSONRPCResponse<*>
        this.synchronize(remoteNode)
        this.peer.addUniqueRemoteNode(remoteNode, response2.result.toString() == "miner")

        logger.info { response2.result }
        return response1.result == "peer"
    }


    fun broadcast(data: JSONRPCRequest): List<JSONRPCResponse<*>> {
        return this.peer.broadcastAll(data)
    }

    fun broadcastMiners(data: JSONRPCRequest): List<JSONRPCResponse<*>> {
        return this.peer.broadcastMiners(data)
    }

}