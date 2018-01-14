package org.vibrant.example.chat.base.producers

import org.vibrant.core.ModelSerializer
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.core.producers.BlockProducer
import org.vibrant.example.chat.base.util.HashUtils
import java.util.*


class BaseBlockProducer(
        private val index: Long,
        private val prevBlockHash: String,
        private val timestamp: Long = Date().time,
        private val transactions: List<BaseTransactionModel>,
        private var nonce: Long = 0,
        private var difficulty: Int = 0
): BlockProducer<BaseBlockModel>(){



    override fun produce(serializer: ModelSerializer): BaseBlockModel {
        val transactionsContent = String(
                transactions
                        .map { serializer.serialize(it) }
                        .fold(byteArrayOf()){a, b -> a + b}
        )



        val hash = if(difficulty == 0){
            val payload = this.index.toString() + this.prevBlockHash + this.timestamp + transactionsContent + this.nonce
            HashUtils.bytesToHex(HashUtils.sha256(payload.toByteArray()))
        }else{
            var hash: String
            do{
                val payload = this.index.toString() + this.prevBlockHash + this.timestamp + transactionsContent + ++this.nonce
                hash = HashUtils.bytesToHex(HashUtils.sha256(payload.toByteArray()))
            }while(hash.substring(0, this.difficulty) != "0".repeat(this.difficulty))
            hash
        }


        return BaseBlockModel(
                index,
                hash,
                prevBlockHash,
                timestamp,
                transactions.toList(),
                this.nonce
        )
    }
}