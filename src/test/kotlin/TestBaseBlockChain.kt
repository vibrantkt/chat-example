import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseMessageModel
import org.vibrant.example.chat.base.producers.BaseBlockChainProducer
import org.vibrant.example.chat.base.producers.BaseTransactionProducer
import org.vibrant.core.algorithm.SignatureProducer
import org.vibrant.example.chat.base.util.AccountUtils
import org.vibrant.example.chat.base.util.HashUtils
import java.security.KeyPair
import java.util.*

class TestBaseBlockChain {


    @Test
    fun `Base blockchain producer`() {
        val sender = AccountUtils.generateKeyPair()
        val receiver = AccountUtils.generateKeyPair()

        val transaction1 = BaseTransactionProducer(
                "User1",
                "User2",
                BaseMessageModel("HElloo, user2", Date().time),
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

        val chain = BaseBlockChainProducer()
        val pushedBlock = chain.pushBlock(chain.createBlock(listOf(transaction1, transaction2), BaseJSONSerializer))


        assertEquals(
                2,
                pushedBlock.transactions.size
        )
    }


    @Test
    fun `Base blockchain serialization`(){
        val sender = AccountUtils.generateKeyPair()
        val receiver = AccountUtils.generateKeyPair()

        val transaction1 = BaseTransactionProducer(
                "User1",
                "User2",
                BaseMessageModel("Hello user 2", 0),
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

        val chain = BaseBlockChainProducer()
        val b = chain.pushBlock(chain.createBlock(listOf(transaction1, transaction2), BaseJSONSerializer, startNonce = 0, timestamp = 0))


        val serialized = BaseJSONSerializer.serialize(chain.produce(BaseJSONSerializer))
        assertEquals(
                "{\"@type\":\"blockchain\",\"blocks\":[{\"@type\":\"block\",\"index\":0,\"hash\":\"Genesis block hash\",\"prevHash\":\"\",\"timestamp\":0,\"transactions\":[],\"nonce\":${0}},{\"@type\":\"block\",\"index\":1,\"hash\":\"${b.hash}\",\"prevHash\":\"Genesis block hash\",\"timestamp\":${0},\"transactions\":[{\"@type\":\"transaction\",\"from\":\"User1\",\"to\":\"User2\",\"payload\":{\"@type\":\"message\",\"content\":\"Hello user 2\",\"timestamp\":${0}},\"signature\":\"${transaction1.signature}\"},{\"@type\":\"transaction\",\"from\":\"User2\",\"to\":\"User1\",\"payload\":{\"@type\":\"message\",\"content\":\"Well, hello!\",\"timestamp\":${0}},\"signature\":\"${transaction2.signature}\"}],\"nonce\":${b.nonce}}]}",
                serialized
        )
    }

    @Test
    fun `Base blockchain deserialization`(){
        val sender = AccountUtils.generateKeyPair()
        val receiver = AccountUtils.generateKeyPair()

        val transaction1 = BaseTransactionProducer(
                "User1",
                "User2",
                BaseMessageModel("Hello user 2", 0),
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

        val chain = BaseBlockChainProducer()
        chain.pushBlock(chain.createBlock(listOf(transaction1, transaction2), BaseJSONSerializer))


        val serialized = BaseJSONSerializer.serialize(chain.produce(BaseJSONSerializer))

        val deserialized = BaseJSONSerializer.deserialize(serialized)

        assertEquals(
                chain.produce(BaseJSONSerializer),
                deserialized
        )
    }


    @Test
    fun `Base blockchain integrity check`(){
        val sender = AccountUtils.generateKeyPair()
        val receiver = AccountUtils.generateKeyPair()

        val transaction1 = BaseTransactionProducer(
                "User1",
                "User2",
                BaseMessageModel("Well, hello1!", Date().time),
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

        val chain = BaseBlockChainProducer()
        chain.pushBlock(chain.createBlock(listOf(transaction1, transaction2), BaseJSONSerializer))

        assertEquals(
                true,
                chain.checkIntegrity()
        )

        chain.blocks[0] = BaseBlockModel(
                0,
                "I CHANGED HASH!",
                "",
                0,
                listOf(),
                0
        )
        assertEquals(
                false,
                chain.checkIntegrity()
        )


    }
}