package org.vibrant.example.chat.base.producers


import org.vibrant.core.serialization.ModelSerializer
import org.vibrant.core.producers.TransactionProducer

import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.example.chat.base.models.TransactionPayload
import org.vibrant.example.chat.base.util.HashUtils
import org.vibrant.example.chat.base.util.serialize
import java.security.KeyPair
import org.vibrant.core.hash.SignatureProducer

/***
 * [BaseTransactionModel] producer class
 *
 * @property from address of sender
 * @property to address of receiver
 * @property payload transaction payload
 * @property keyPair keyPair, which will be used to create signature
 * @property signatureProducer created signature
 *
 */
open class BaseTransactionProducer(
        private val from: String,
        private val to: String,
        private val payload: TransactionPayload,
        private val keyPair: KeyPair,
        private val signatureProducer: SignatureProducer
): TransactionProducer<BaseTransactionModel>() {
    override fun produce(serializer: ModelSerializer): BaseTransactionModel {
        return BaseTransactionModel(
                from,
                to,
                HashUtils.bytesToHex(BaseJSONSerializer.serialize(payload) + from.toByteArray() + to.toByteArray()),
                payload,
                HashUtils.bytesToHex(
                        signatureProducer.produceSignature((this.from + this.to + String(serializer.serialize(this.payload))).toByteArray(), keyPair)
                )
        )
    }
}
