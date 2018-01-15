import org.junit.Assert.assertEquals
import org.junit.Test
import org.vibrant.core.algorithm.SignatureProducer
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.models.BaseMessageModel
import org.vibrant.example.chat.base.producers.BaseBlockProducer
import org.vibrant.example.chat.base.producers.BaseTransactionProducer
import org.vibrant.example.chat.base.util.AccountUtils
import org.vibrant.example.chat.base.util.HashUtils
import org.vibrant.example.chat.base.util.serialize
import java.security.KeyPair
import java.util.*

class TestBaseBlock {


    @Test
    fun `Base block producer`() {
        val sender = AccountUtils.generateKeyPair()
        val receiver = AccountUtils.generateKeyPair()

        val transaction1 = BaseTransactionProducer(
                "User1",
                "User2",
                BaseMessageModel("Hello, user2!!", Date().time),
                sender,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)

        val transaction2 = BaseTransactionProducer(
                "User2",
                "User1",
                BaseMessageModel("Well, hello", Date().time),
                receiver,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)
        val block = BaseBlockProducer(
                1,
                "prevBlockHash",
                1000,
                listOf(transaction1, transaction2),
                nonce = 0,
                difficulty = 0
        ).produce(BaseJSONSerializer)



        assertEquals(
                1000,
                block.timestamp
        )

        assertEquals(
                1,
                block.index
        )

        assertEquals(
                "prevBlockHash",
                block.prevHash
        )

        val payload =
                block.index.toString() +
                        block.prevHash +
                        block.timestamp +
                        block.transactions.map{ it.serialize() }.reduceRight({a, b -> a + b}) +
                        block.nonce

        assertEquals(
                0,
                block.nonce
        )

        assertEquals(
                HashUtils.bytesToHex(
                        HashUtils.sha256(payload.toByteArray())
                ),
                block.hash
        )

    }


    @Test
    fun `Base block serialization`(){
        val sender = AccountUtils.generateKeyPair()
        val receiver = AccountUtils.generateKeyPair()

        val transaction1 = BaseTransactionProducer(
                "User1",
                "User2",
                BaseMessageModel("Hello, user2!!", 0),
                sender,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)

        val transaction2 = BaseTransactionProducer(
                "User2",
                "User1",
                BaseMessageModel("Well, hello!", 0),
                receiver,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)
        val block = BaseBlockProducer(
                1,
                "prevBlockHash",
                1000,
                listOf(transaction1, transaction2)

        ).produce(BaseJSONSerializer)

        val hash = block.hash

        assertEquals(
                "{\"@type\":\"block\",\"index\":1,\"hash\":\"$hash\",\"prevHash\":\"prevBlockHash\",\"timestamp\":1000,\"transactions\":[{\"@type\":\"transaction\",\"from\":\"User1\",\"to\":\"User2\",\"payload\":{\"@type\":\"message\",\"content\":\"Hello, user2!!\",\"timestamp\":0},\"signature\":\"${transaction1.signature}\"},{\"@type\":\"transaction\",\"from\":\"User2\",\"to\":\"User1\",\"payload\":{\"@type\":\"message\",\"content\":\"Well, hello!\",\"timestamp\":0},\"signature\":\"${transaction2.signature}\"}],\"nonce\":0}",
                block.serialize()
        )
    }

    @Test
    fun `Base block deserialization`(){
        val sender = AccountUtils.generateKeyPair()
        val receiver = AccountUtils.generateKeyPair()

        val transaction1 = BaseTransactionProducer(
                "User1",
                "User2",
                BaseMessageModel("HEllo user 2", Date().time),
                sender,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)

        val transaction2 = BaseTransactionProducer(
                "User2",
                "User1",
                BaseMessageModel("Well, hello!", Date().time),
                receiver,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer)
        val block = BaseBlockProducer(
                1,
                "prevBlockHash",
                1000,
                listOf(transaction1, transaction2)

        ).produce(BaseJSONSerializer)

        val serializedBlock = BaseJSONSerializer.serialize(block)

        val deserializedBlock = BaseJSONSerializer.deserialize(serializedBlock)

        assertEquals(
                block,
                deserializedBlock
        )
    }
}