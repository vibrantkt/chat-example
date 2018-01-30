package org.vibrant.example.chat.base.producers

import org.vibrant.core.ModelSerializer
import org.vibrant.core.database.blockchain.InMemoryBlockChain
import org.vibrant.example.chat.base.models.BaseBlockChainModel
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import java.util.*
import kotlin.collections.ArrayList

class BaseBlockChainProducer(val difficulty: Int = 1) : InMemoryBlockChain<BaseBlockModel, BaseBlockChainModel>(){


    override fun produce(serializer: ModelSerializer): BaseBlockChainModel {
        return BaseBlockChainModel(
                difficulty,
                blocks
        )
    }

    fun createBlock(transactions: List<BaseTransactionModel>, serializer: ModelSerializer, startNonce: Long = 0, timestamp: Long = Date().time): BaseBlockModel {
        return BaseBlockProducer(
                this.latestBlock().index + 1,
                this.latestBlock().hash,
                timestamp,
                transactions,
                startNonce,
                this.difficulty
        ).produce(serializer)
    }


    override fun checkIntegrity(): Boolean{
        this.blocks.reduce({a,b ->
            if(a.hash == b.previousHash){
                return@reduce b
            }else{
                return false
            }
        })
        return true
    }

    override fun createGenesisBlock(): BaseBlockModel {
        return BaseBlockModel(
                0,
                "Genesis block hash",
                "",
                0,
                listOf(),
                0
        )
    }



    override fun dump(copy: BaseBlockChainModel){
        this.blocks().clear()
        this.blocks().addAll(copy.blocks)
        this.notifyNewBlock()
    }

    companion object {

        fun instantiate(blockChainModel: BaseBlockChainModel): BaseBlockChainProducer {
            val producer = BaseBlockChainProducer()
            producer.blocks().clear()
            producer.blocks().addAll(blockChainModel.blocks)
            return producer
        }
    }



    internal fun blocks(): ArrayList<BaseBlockModel> {
        return this.blocks
    }

}