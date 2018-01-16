package org.vibrant.example.chat.base.node

import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.example.chat.base.util.serialize
import java.util.*
import kotlin.coroutines.experimental.suspendCoroutine

class BaseMiner(port: Int) : Node(port){


    private val miner = Miner(this)


    suspend fun addTransaction(transactionModel: BaseTransactionModel) = this.miner.addTransaction(transactionModel)

    class Miner(private val baseMiner: BaseMiner){

        private val pendingTransactions = arrayListOf<BaseTransactionModel>()
        private var isMining = false

        suspend fun addTransaction(transactionModel: BaseTransactionModel) = suspendCoroutine<Unit> {
            this@Miner.pendingTransactions.add(transactionModel)
            if(!isMining){
                this@Miner.mine()
                it.resume(Unit)
            }
        }

        private fun mine(){
            isMining = true
            val timestamp = Date().time
            val block = baseMiner.chain.addBlock(baseMiner.chain.createBlock(
                    this.pendingTransactions,
                    BaseJSONSerializer,
                    timestamp = timestamp
            ))
            baseMiner.logger.info { "Block mined" }
            this.pendingTransactions.clear()
            baseMiner.logger.info { "Broadcasting this block..." }
            val response = baseMiner.peer.broadcast(baseMiner.createRequest(
                    "newBlock",
                    arrayOf(block.serialize())
            ))
            baseMiner.logger.info { "Awaited this shit! $response" }
            if(this.pendingTransactions.isNotEmpty()){
                this.mine()
            }
            isMining = false
        }
    }
}