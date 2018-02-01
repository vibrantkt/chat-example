package org.vibrant.example.chat.base.node

import org.vibrant.core.database.blockchain.BlockChain
import org.vibrant.core.hash.SignatureProducer
import org.vibrant.core.node.JSONRPCNode
import org.vibrant.core.node.RemoteNode
import org.vibrant.core.rpc.json.JSONRPCResponse
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.models.*
import org.vibrant.example.chat.base.producers.BaseBlockChainProducer
import org.vibrant.example.chat.base.producers.BaseTransactionProducer
import org.vibrant.example.chat.base.util.*
import java.security.KeyPair
import java.util.*


@Suppress("USELESS_IS_CHECK")
open class Node(port: Int) : JSONRPCNode<Peer>() {


    @Suppress("LeakingThis") internal val rpc = BaseJSONRPCProtocol(this)

    override val peer = Peer(port, rpc)

    internal val chain: BaseBlockChainProducer = BaseBlockChainProducer(difficulty = 2)

    internal var keyPair = AccountUtils.generateKeyPair()

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



    fun synchronize(remoteNode: RemoteNode){
        this.rpc.synchronize(remoteNode)
    }



    fun message(to: String, content: String, keyPair: KeyPair, sync: Boolean = true) = this.transaction(to, BaseMessageModel(content, Date().time), keyPair, sync)
    fun changeName(to: String, name: String, keyPair: KeyPair, sync: Boolean = true) = this.transaction(to, org.vibrant.example.chat.base.models.BaseAccountMetaDataModel(name, Date().time), keyPair, sync)



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

    private fun transaction(to: String, payload: TransactionPayload, keyPair: KeyPair, @Suppress("UNUSED_PARAMETER") sync: Boolean = true): List<JSONRPCResponse<*>>{
        val transaction = createTransaction(to, payload, keyPair)
        logger.info { "Transaction prepared: ${BaseJSONSerializer.serializeToString(transaction)}" }
        logger.info { this.peer.peers }
        return this.peer.broadcast(
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