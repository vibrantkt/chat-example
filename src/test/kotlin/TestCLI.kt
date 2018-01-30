import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import org.junit.Assert.assertEquals
import org.junit.Test
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.Chat
import org.vibrant.example.chat.base.models.BaseBlockChainModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.example.chat.base.util.AccountUtils
import org.vibrant.example.chat.base.util.HashUtils
import java.io.File
import java.util.*

@Suppress("UNCHECKED_CAST")
class TestCLI {




    @Test
    fun `Test connection`(){
        val chat1 = Chat()
        val chat2 = Chat()


        chat1.handleCommand("connect localhost:${chat2.node.peer.port}")


        assertEquals(
                1,
                chat1.node.peer.peers.size
        )

        assertEquals(
                1,
                chat2.node.peer.peers.size
        )


        chat1.stop()
        chat2.stop()
    }


    @Test
    fun `Test authorization`(){
        val chat1 = Chat()

        val tmpFile = File.createTempFile("authKey", ".rsa")
        val keyPair = AccountUtils.generateKeyPair()
        tmpFile.writeBytes(AccountUtils.serializeKeyPair(keyPair))

        chat1.handleCommand("auth ${tmpFile.absolutePath}")

        assertEquals(
                chat1.keyPair.private,
                keyPair.private
        )

        chat1.stop()
    }


    @Test
    fun `Test transaction`(){
        val chat1 = Chat()
        val miner = Chat(true)



        chat1.keyPair = AccountUtils.generateKeyPair()
        miner.keyPair = AccountUtils.generateKeyPair()


        chat1.handleCommand("connect localhost:${miner.node.peer.port}")


        assertEquals(
                0,
                miner.node.peer.miners.size
        )

        assertEquals(
                1,
                chat1.node.peer.miners.size
        )

        chat1.handleCommand("transaction ${HashUtils.bytesToHex(miner.keyPair.public.encoded)} hello")
        chat1.waitBlock({true})
        assertEquals(
                miner.node.chain.produce(BaseJSONSerializer),
                chat1.node.chain.produce(BaseJSONSerializer)
        )

        assertEquals(
                2,
                miner.node.chain.blocks().size
        )

        assertEquals(
                2,
                chat1.node.chain.blocks().size
        )


        miner.stop()

        chat1.stop()
        miner.stop()
    }


    @Test
    fun `Test account name transaction`(){
        val chat1 = Chat()
        val miner = Chat(true)

        chat1.keyPair = AccountUtils.generateKeyPair()
        miner.keyPair = AccountUtils.generateKeyPair()


        chat1.handleCommand("connect localhost:${miner.node.peer.port}")


        assertEquals(
                0,
                miner.node.peer.miners.size
        )

        assertEquals(
                1,
                chat1.node.peer.miners.size
        )

        chat1.handleCommand("account NewName")
        chat1.waitBlock({true})



        assertEquals(
                miner.node.chain.produce(BaseJSONSerializer),
                chat1.node.chain.produce(BaseJSONSerializer)
        )

        assertEquals(
                2,
                miner.node.chain.blocks().size
        )

        assertEquals(
                2,
                chat1.node.chain.blocks().size
        )

        miner.stop()

        chat1.stop()
        miner.stop()
    }


    @Test
    fun `Test http`(){
        val chat1 = Chat()
        val chat2 = Chat()
        val miner = Chat(true)

        chat1.keyPair = AccountUtils.generateKeyPair()
        chat2.keyPair = AccountUtils.generateKeyPair()
        miner.keyPair = AccountUtils.generateKeyPair()


        chat1.handleCommand("connect localhost:${miner.node.peer.port}")
        chat2.handleCommand("connect localhost:${miner.node.peer.port}")


        assertEquals(
                0,
                miner.node.peer.miners.size
        )

        assertEquals(
                1,
                chat1.node.peer.miners.size
        )

        assertEquals(
                1,
                chat2.node.peer.miners.size
        )

        chat1.handleCommand("transaction ${HashUtils.bytesToHex(chat2.keyPair.public.encoded)} hello")

        chat1.waitBlock({true})
        chat2.waitBlock({true})


        assertEquals(
                miner.node.chain.produce(BaseJSONSerializer),
                chat1.node.chain.produce(BaseJSONSerializer)
        )

        assertEquals(
                miner.node.chain.produce(BaseJSONSerializer),
                chat2.node.chain.produce(BaseJSONSerializer)
        )

        assertEquals(
                2,
                miner.node.chain.blocks().size
        )

        assertEquals(
                2,
                chat1.node.chain.blocks().size
        )
        assertEquals(
                2,
                chat2.node.chain.blocks().size
        )


        val(_, _, result2) =  "http://localhost:${chat1.http.port()}/messages".httpGet().responseString()
        when(result2){
            is Result.Success -> {
                val map: HashMap<String, Any> = jacksonObjectMapper().readValue(result2.get(), object : TypeReference<Map<String, Any>>(){})

                val transactions = (map["messages"] as List<BaseTransactionModel>)
                assertEquals(
                        1,
                        transactions.size
                )
            }
            else -> {}
        }

        chat1.handleCommand("transaction ${HashUtils.bytesToHex(chat2.keyPair.public.encoded)} hellothere")

        chat1.waitBlock({true})
        chat2.waitBlock({true})


        assertEquals(
                miner.node.chain.produce(BaseJSONSerializer),
                chat1.node.chain.produce(BaseJSONSerializer)
        )

        assertEquals(
                miner.node.chain.produce(BaseJSONSerializer),
                chat2.node.chain.produce(BaseJSONSerializer)
        )

        assertEquals(
                3,
                miner.node.chain.blocks().size
        )

        assertEquals(
                3,
                chat1.node.chain.blocks().size
        )
        assertEquals(
                3,
                chat2.node.chain.blocks().size
        )


        val(_, _, result) =  "http://localhost:${chat1.http.port()}/messages".httpGet().responseString()
        when(result){
            is Result.Success -> {
                val map: HashMap<String, Any> = jacksonObjectMapper().readValue(result.get(), object : TypeReference<Map<String, Any>>(){})

                val transactions = (map["messages"] as List<BaseTransactionModel>)
                assertEquals(
                        2,
                        transactions.size
                )

                println(result.get())
            }
            else -> {
                println("Fuckin ghsit")
            }
        }

        chat1.stop()
        chat2.stop()
        miner.stop()

    }

    @Test
    fun `Test full client`(){
        val chat1 = Chat()
        chat1.setAccount(AccountUtils.generateKeyPair())

        val chat2 = Chat()
        chat2.setAccount(AccountUtils.generateKeyPair())


        val miner = Chat(true)


        fun command(peer: Chat, command: String): String {
            val(_, _, result) =  "http://localhost:${peer.http.port()}/command".httpPost().body("{\"command\":\"$command\"}").responseString()
            return result.get()
        }

        assertEquals(
                "true",
                command(chat1, "connect localhost:${chat2.node.peer.port}")
        )
        assertEquals(
                "true",
                command(chat1, "connect localhost:${miner.node.peer.port}")
        )
        assertEquals(
                "true",
                command(chat2, "connect localhost:${miner.node.peer.port}")
        )

        assertEquals(
                2,
                chat1.node.peer.peers.size
        )

        assertEquals(
                1,
                chat1.node.peer.miners.size
        )




        assertEquals(
                "true",
                command(chat1, "transaction ${chat2.hexAddress()} hello!")
        )

        chat1.waitBlock({true})
        chat2.waitBlock({true})

        assertEquals(
                2,
                miner.node.chain.blocks().size
        )

        assertEquals(
                2,
                chat1.node.chain.blocks().size
        )

        assertEquals(
                2,
                chat2.node.chain.blocks().size
        )


        val(_, _, result) =  "http://localhost:${chat1.http.port()}/blockchain".httpGet().response()

        val bchain = BaseJSONSerializer.deserialize(result.get()) as BaseBlockChainModel

        println(bchain)

        assertEquals(
                2,
                bchain.blocks.size
        )

        val(_, _, result2) =  "http://localhost:${chat2.http.port()}/blockchain".httpGet().response()

        val bchain2 = BaseJSONSerializer.deserialize(result2.get()) as BaseBlockChainModel
        assertEquals(
                2,
                bchain2.blocks.size
        )



        chat1.stop()
        chat2.stop()
        miner.stop()
    }
}