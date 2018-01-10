package org.vibrant.example.chat.base.node

import kotlinx.coroutines.experimental.runBlocking
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import java.util.*
import java.util.concurrent.CountDownLatch

class BaseMiner(port: Int) : BaseNode(port){


    private var awaitTransactionsLatch = CountDownLatch(1)

    private val pendingTransactions = arrayListOf<BaseTransactionModel>()


    internal val onMined = arrayListOf<(BaseBlockModel) -> Unit>()

    fun addTransaction(transactionModel: BaseTransactionModel){
        this.pendingTransactions.add(transactionModel)
        logger.info { "I GOT A FUCKING TRANSACTION!!!!" }
        awaitTransactionsLatch.countDown()
    }



    internal fun mine(){
        val timestamp = Date().time
        val block = this.chain.pushBlock(this.chain.createBlock(
                this.pendingTransactions,
                BaseJSONSerializer(),
                timestamp = timestamp
        ))
        logger.info { "Block mined" }
        this.pendingTransactions.clear()
        runBlocking {
            logger.info { "Broadcasting this block..." }
            val response = this@BaseMiner.peer.broadcastAll(JSONRPCRequest(
                    method = "newBlock",
                    params = arrayOf(BaseJSONSerializer().serialize(block)),
                    id = this@BaseMiner.requestID++
            ))
            logger.info { "Awaited this shit! $response" }
        }
        this.onMined.forEach {
            logger.info { "Notifying handlers..." }
            it(block)
        }
    }


    internal fun startMineLoop() {
        while(true){
            awaitTransactionsLatch.await()
            this@BaseMiner.mine()
            awaitTransactionsLatch = CountDownLatch(1)
        }
    }
}