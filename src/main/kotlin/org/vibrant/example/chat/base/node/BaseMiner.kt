package org.vibrant.example.chat.base.node

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.example.chat.base.producers.BaseBlockChainProducer
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

class BaseMiner(port: Int) : BaseNode(port){


    private val miner = Miner(this)

    private var continuation: Continuation<Unit>? = null

    private val pendingTransactions = arrayListOf<BaseTransactionModel>()

    abstract class OnMinedListener{
        abstract fun handle(blockModel: BaseBlockModel)
    }

    internal val onMined = arrayListOf<OnMinedListener>()

    fun addTransaction(transactionModel: BaseTransactionModel){
        runBlocking {
            this@BaseMiner.miner.addTransaction(transactionModel)
        }
    }


    class Miner(private val baseMiner: BaseMiner){

        private val pendingTransactions = arrayListOf<BaseTransactionModel>()
        private var isMining = false

        suspend fun addTransaction(transactionModel: BaseTransactionModel) = suspendCoroutine<Unit> {
            if(isMining){
                this@Miner.pendingTransactions.add(transactionModel)
            }else{
                this@Miner.mine()
                it.resume(Unit)
            }
        }

        private fun mine(){
            isMining = true
            val timestamp = Date().time
            val block = baseMiner.chain.pushBlock(baseMiner.chain.createBlock(
                    this.pendingTransactions,
                    BaseJSONSerializer,
                    timestamp = timestamp
            ))
            baseMiner.logger.info { "Block mined" }
            this.pendingTransactions.clear()
            baseMiner.logger.info { "Broadcasting this block..." }
            val response = baseMiner.peer.broadcastAll(JSONRPCRequest(
                    method = "newBlock",
                    params = arrayOf(BaseJSONSerializer.serialize(block)),
                    id = baseMiner.requestID++
            ))
            baseMiner.logger.info { "Awaited this shit! $response" }
            if(this.pendingTransactions.isNotEmpty()){
                this.mine()
            }
            isMining = false
        }
    }
}