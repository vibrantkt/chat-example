package org.vibrant.example.chat.base.producers

import org.vibrant.core.ModelSerializer
import org.vibrant.example.chat.base.models.BaseBlockChainModel
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.core.producers.BlockChainProducer
import java.util.*

class BaseBlockChainProducer(val difficulty: Int = 1) : BlockChainProducer<BaseBlockChainModel>(){

    val blocks = arrayListOf(
            this.createGenesisBlock()
    )

    internal val onChange = arrayListOf<(BlockChainProducer<BaseBlockChainModel>) -> Unit>()

    private val listeners = arrayListOf<NewBlockListener>()
    fun addNewBlockListener(newBlockListener: NewBlockListener){
        this.listeners.add(newBlockListener)
    }

    override fun produce(serializer: ModelSerializer): BaseBlockChainModel {
        return BaseBlockChainModel(
                difficulty,
                blocks
        )
    }


    fun latestBlock(): BaseBlockModel {
        return this.blocks.last()
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


    fun pushBlock(block: BaseBlockModel): BaseBlockModel {
        this.blocks.add(block)
        this.handleChange()
        return this.latestBlock()
    }


    fun checkIntegrity(): Boolean{
        this.blocks.reduce({a,b ->
            if(a.hash == b.prevHash){
                return@reduce b
            }else{
                return false
            }
        })
        return true
    }

    private fun createGenesisBlock(): BaseBlockModel {
        return BaseBlockModel(
                0,
                "Genesis block hash",
                "",
                0,
                listOf(),
                0
        )
    }



    fun dump(blockChainModel: BaseBlockChainModel){
        this.blocks.clear()
        this.blocks.addAll(blockChainModel.blocks)
        this.handleChange()
    }


    private fun handleChange(){
        this.onChange.forEach { it(this@BaseBlockChainProducer) }
        this.listeners.forEach{ it.nextBlock(this.latestBlock()) }
    }

    companion object {

        fun instantiate(blockChainModel: BaseBlockChainModel): BaseBlockChainProducer {
            val producer = BaseBlockChainProducer()
            producer.blocks.clear()
            producer.blocks.addAll(blockChainModel.blocks)
            return producer
        }
    }




    abstract class NewBlockListener{
        abstract fun nextBlock(blockModel: BaseBlockModel)
    }



}