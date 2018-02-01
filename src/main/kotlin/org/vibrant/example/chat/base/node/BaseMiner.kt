package org.vibrant.example.chat.base.node

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import org.vibrant.core.database.blockchain.BlockChain
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseMessageModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.example.chat.base.util.serialize
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.experimental.suspendCoroutine

class BaseMiner(port: Int) : Node(port){


    private val miner = Miner(this)


    fun addTransaction(transactionModel: BaseTransactionModel){
        this.miner.addTransaction(transactionModel)
    }

    class Miner(private val baseMiner: BaseMiner): EventListener{

        private val logger = KotlinLogging.logger {  }
        private val pendingTransactions = arrayListOf<BaseTransactionModel>()
        private var isMining = true
        private val listeners = arrayListOf<(BaseBlockModel) -> Unit>()

        private var minerLoop: Deferred<Unit>? = async {
            this@Miner.mine()
        }



        private fun blockMined(blockModel: BaseBlockModel){
            logger.info { "Block mined $blockModel" }
//            this.listeners.toList().forEach{
//                        try{
//                            it.invoke(blockModel)
//                        } catch (e: Exception){}
//                    }
//            this.listeners.clear()
        }

        fun addTransaction(transactionModel: BaseTransactionModel){
            this@Miner.pendingTransactions.add(transactionModel)
            logger.info { "Adding transaction..." }
        }

        private fun mine(){
            async(newSingleThreadContext("miner loop")){
                while(this@Miner.isMining){
                    val timestamp = Date().time
                    val selectedTransactions = this@Miner.pendingTransactions.toList()
                    if(selectedTransactions.isNotEmpty()){
                        logger.info { "Got pending transactions" }

                        val block = baseMiner.chain.addBlock(baseMiner.chain.createBlock(
                                selectedTransactions,
                                BaseJSONSerializer,
                                timestamp = timestamp
                        ))


                        selectedTransactions.forEach { this@Miner.pendingTransactions.remove(it) }


                        logger.info { "Broadcasting" }

                        val response = baseMiner.peer.broadcast(baseMiner.createRequest(
                                "onNewBlock",
                                arrayOf(block.serialize())
                        ))

                        logger.info { "Broadcasted" }
                        this@Miner.blockMined(block)
                        baseMiner.logger.info { "Awaited this shit! $response" }
                    }
                }
            }
        }
    }
}