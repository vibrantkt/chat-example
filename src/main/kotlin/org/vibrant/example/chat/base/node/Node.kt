package org.vibrant.example.chat.base.node

import org.vibrant.base.database.blockchain.BlockChain
import org.vibrant.base.node.JSONRPCNode
import org.vibrant.base.rpc.json.JSONRPCResponse
import org.vibrant.core.algorithm.SignatureProducer
import org.vibrant.core.node.RemoteNode
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


    private fun getSmartContract(address: String): DeployContractModel? {
        return this.chain.blocks().flatMap{ it.transactions }.filter {
            it.payload is DeployContractModel
        }.map {
            it.payload as DeployContractModel
        }.findLast {
            it.contract.address == address
        }
    }

    private fun getSmartContractOf(address: String): DeployContractModel? {
        return this.chain.blocks().flatMap{ it.transactions }.filter {
            it.payload is DeployContractModel
        }.map {
            it.payload as DeployContractModel
        }.findLast {
            it.contract.author == address
        }
    }

    init {
        this.chain.addNewBlockListener(object: BlockChain.NewBlockListener<BaseBlockModel>{
            override fun nextBlock(blockModel: BaseBlockModel) {
                logger.info { "On next blockkkk" }
                blockModel.transactions.forEach{ transaction ->
                    when(transaction.payload){
                        is TriggerContractModel -> {
                            println("trigger ${transaction.payload}")
                            val contractAddress = transaction.to
                            val contract = getSmartContract(contractAddress)
                            if (contract != null) {
                                if(contract.contract.triggerWord == (transaction.payload.message.payload as BaseMessageModel).content && contract.contract.author == transaction.payload.message.to){
                                    println("Trigger worked, responding...")
                                    this@Node.message(
                                            transaction.payload.message.from,
                                            contract.contract.response,
                                            keyPair
                                    )
                                }
                            }
                        }
                        is BaseMessageModel -> {
                            // we need to trigger smart contract
                            val c = getSmartContractOf(transaction.to)
                            if (c != null) {
                                logger.info { "Triggering contract..." }
                                this@Node.triggerContract(
                                        c.contract.address,
                                        transaction,
                                        this@Node.keyPair
                                )
                                logger.info { "Triggered contract!" }
                            }
                        }
                        else -> {
                            //don't care
                        }
                    }
                }
            }
        })
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
    fun deployContract(contract: EchoHelloContract, keyPair: KeyPair, sync: Boolean = true): List<JSONRPCResponse<*>> {
        return this.transaction(
                contract.address,
                DeployContractModel(contract, Date().time),
                keyPair, sync
        )
    }
    fun triggerContract(contract: String, message: BaseTransactionModel, keyPair: KeyPair, sync: Boolean = true): List<JSONRPCResponse<*>> {
        return this.transaction(
                contract,
                TriggerContractModel(message, Date().time),
                keyPair, sync
        )
    }


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