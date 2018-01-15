package org.vibrant.example.chat.base.node

import org.vibrant.base.node.JSONRPCNode
import org.vibrant.base.rpc.json.JSONRPCRequest
import org.vibrant.core.algorithm.SignatureProducer
import org.vibrant.core.node.RemoteNode
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse
import org.vibrant.example.chat.base.models.*
import org.vibrant.example.chat.base.producers.BaseBlockChainProducer
import org.vibrant.example.chat.base.producers.BaseTransactionProducer
import org.vibrant.example.chat.base.util.HashUtils
import org.vibrant.example.chat.base.util.deserialize
import org.vibrant.example.chat.base.util.serialize
import org.vibrant.example.chat.base.util.stringResult
import java.security.KeyPair
import java.util.*


@Suppress("USELESS_IS_CHECK")
open class Node(port: Int) : JSONRPCNode<Peer>() {


    @Suppress("LeakingThis") internal val rpc = BaseJSONRPCProtocol(this)

    override val peer = Peer(port, rpc)

    internal val chain: BaseBlockChainProducer = BaseBlockChainProducer(difficulty = 2)

    override fun start() {
        this.peer.start()
    }

    override fun stop() {
        this.peer.stop()
    }

    override fun connect(remoteNode: RemoteNode): Boolean {
        val response2 = this@Node.request(createRequest("nodeType", arrayOf(this.nodeType)), remoteNode)
        this.synchronize(remoteNode)
        this.peer.addUniqueRemoteNode(remoteNode, response2.stringResult() == "miner")

        logger.info { response2.result }
        return super.connect(remoteNode)
    }

    fun handleLastBlock(lastBlock: BaseBlockModel, remoteNode: RemoteNode){
        val localLatestBlock = this@Node.chain.latestBlock()
        if(localLatestBlock != lastBlock){
            logger.info { "My chain is not in sync with peer $remoteNode" }
            when {
                // next block
                lastBlock.index - localLatestBlock.index == 1L && lastBlock.prevHash == localLatestBlock.hash -> {
                    this@Node.chain.pushBlock(
                            lastBlock
                    )
                    logger.info { "I just got next block. Chain good: ${chain.checkIntegrity()}" }
                }
                // block is ahead
                lastBlock.index > localLatestBlock.index -> {
                    logger.info { "My chain is behind, requesting full chain" }
                    val chainResponse = this@Node.request(
                            this.createRequest("getFullChain", arrayOf()),
                            remoteNode
                    )

                    val model = chainResponse.deserialize<BaseBlockChainModel>()
                    val tmpChain = BaseBlockChainProducer.instantiate(model)
                    val chainOK = tmpChain.checkIntegrity()

                    if(chainOK){
                        logger.info { "Received chain is fine, replacing" }
                        this@Node.chain.dump(model)
                        logger.info { "Received chain is fine, replaced" }
                    }else{
                        logger.info { "Received chain is not fine, I ignore it" }
                    }
                }
                // block is behind
                else -> {
                    logger.info { "My chain is ahead, sending request" }
                    val response = this@Node.request(this.createRequest("syncWithMe", arrayOf()), remoteNode)
                    logger.info { "Got response! $response" }
                }
            }
        }else{
            logger.info { "Chain in sync with peer $remoteNode" }
        }
    }


    fun synchronize(remoteNode: RemoteNode){
        logger.info { "Requesting and waiting for response get last block" }
        val response = this@Node.request(createRequest("getLastBlock", arrayOf()), remoteNode)
        logger.info { "Got last block ${response.result}" }
        val lastBlock = response.deserialize<BaseBlockModel>()
        logger.info { "Last block is $lastBlock" }
        this@Node.handleLastBlock(lastBlock, remoteNode)
        logger.info { "Handled" }
    }



    fun message(to: String, content: String, keyPair: KeyPair) = this.transaction(to, BaseMessageModel(content, Date().time), keyPair)
    fun changeName(to: String, name: String, keyPair: KeyPair) = this.transaction(to, org.vibrant.example.chat.base.models.BaseAccountMetaDataModel(name, Date().time), keyPair)


    private fun createTransaction(to: String, payload: TransactionPayload, keyPair: KeyPair): BaseTransactionModel {
        return BaseTransactionProducer(
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
    }

    private fun transaction(to: String, payload: TransactionPayload, keyPair: KeyPair): List<JSONRPCResponse<*>>{
        val transaction = createTransaction(to, payload, keyPair)

        return this.peer.broadcastMiners(
                this.createRequest("addTransaction", arrayOf(
                        transaction.serialize()
                ))
        )
    }

    private val nodeType
        get() = when(this){
            is BaseMiner -> "miner"
            is Node -> "node"
            else -> "node"
        }

}