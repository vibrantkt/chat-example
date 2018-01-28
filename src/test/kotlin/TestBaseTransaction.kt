import org.junit.Assert.assertEquals
import org.junit.Test
import org.vibrant.example.chat.base.producers.BaseTransactionProducer
import org.vibrant.core.algorithm.SignatureProducer
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.models.BaseAccountMetaDataModel
import org.vibrant.example.chat.base.models.BaseMessageModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.example.chat.base.util.AccountUtils
import org.vibrant.example.chat.base.util.HashUtils
import org.vibrant.example.chat.base.util.serialize
import java.security.KeyPair

class TestBaseTransaction {



    @Test
    fun `Base transaction producer`() {
        val sender = AccountUtils.generateKeyPair()
        val transaction = BaseTransactionProducer(
                "yura",
                "vasya",
                BaseAccountMetaDataModel("Yurii", 0),
                sender,
                object : SignatureProducer{
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)

        assertEquals(
                "yura",
                transaction.from
        )

        assertEquals(
                "vasya",
                transaction.to
        )

        assertEquals(
                BaseAccountMetaDataModel("Yurii", 0),
                transaction.payload
        )

        assertEquals(
                HashUtils.bytesToHex(AccountUtils.signData("yuravasya" + BaseAccountMetaDataModel("Yurii", 0).serialize(), sender)),
                transaction.signature
        )

    }


    @Test
    fun `Base transaction deserialization`() {
        val sender = AccountUtils.generateKeyPair()
        val transaction = BaseTransactionProducer(
                "yura",
                "vasya",
                BaseMessageModel("Hello!", 0),
                sender,
                object : SignatureProducer{
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)

        val converted = BaseJSONSerializer.deserialize(BaseJSONSerializer.serialize(transaction)) as BaseTransactionModel

        assertEquals(
                "yura",
                converted.from
        )

        assertEquals(
                "vasya",
                converted.to
        )

        assertEquals(
                BaseMessageModel("Hello!", 0),
                converted.payload
        )

        assertEquals(
                HashUtils.bytesToHex(AccountUtils.signData("yuravasya" + BaseMessageModel("Hello!",0).serialize(), sender)),
                converted.signature
        )
    }

    @Test
    fun `Base transaction serialization`() {
        val sender = AccountUtils.generateKeyPair()
        val transaction = BaseTransactionProducer(
                "yura",
                "vasya",
                BaseMessageModel("Hello!", 0),
                sender,
                object : SignatureProducer{
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)

        val serialized = BaseJSONSerializer.serialize(transaction)
        assertEquals(
                "{\"@type\":\"transaction\",\"from\":\"yura\",\"to\":\"vasya\",\"hash\":\"${transaction.hash}\",\"payload\":{\"@type\":\"message\",\"content\":\"Hello!\",\"timestamp\":0},\"signature\":\"${transaction.signature}\"}",
                String(serialized)
        )
    }
}