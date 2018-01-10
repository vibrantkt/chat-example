import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.Chat
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.example.chat.base.node.BaseMiner
import org.vibrant.example.chat.base.util.AccountUtils
import org.vibrant.example.chat.base.util.HashUtils
import java.io.File
import java.util.HashMap
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.experimental.suspendCoroutine

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

    }


    @Test
    fun `Test authorization`(){
        val chat1 = Chat()

        val tmpFile = File.createTempFile("authKey", ".rsa")
        val keyPair = AccountUtils.generateKeyPair()
        tmpFile.writeBytes(AccountUtils.serializeKeyPair(keyPair))

        chat1.handleCommand("auth ${tmpFile.absolutePath}")


    }


    @Test
    fun `Test transaction`(){
        val chat1 = Chat()
        val miner = Chat(true)

        async {
            (miner.node as BaseMiner).startMineLoop()
        }


        chat1.node.keyPair = AccountUtils.generateKeyPair()
        miner.node.keyPair = AccountUtils.generateKeyPair()


        chat1.handleCommand("connect localhost:${miner.node.peer.port}")


        assertEquals(
                0,
                miner.node.peer.miners.size
        )

        assertEquals(
                1,
                chat1.node.peer.miners.size
        )

        chat1.handleCommand("transaction ${HashUtils.bytesToHex(miner.node.keyPair!!.public.encoded)} hello")


        runBlocking {
            suspendCoroutine<Unit>{ r->
                (miner.node as BaseMiner).onMined.add{
                    r.resume(Unit)
                }
            }


            assertEquals(
                    miner.node.chain.produce(BaseJSONSerializer()),
                    chat1.node.chain.produce(BaseJSONSerializer())
            )

            assertEquals(
                    2,
                    miner.node.chain.blocks.size
            )

            assertEquals(
                    2,
                    chat1.node.chain.blocks.size
            )
        }

    }


    @Test
    fun `Test http`(){
        val chat1 = Chat()
        val chat2 = Chat()
        val miner = Chat(true)

        async {
            (miner.node as BaseMiner).startMineLoop()
        }


        chat1.node.keyPair = AccountUtils.generateKeyPair()
        chat2.node.keyPair = AccountUtils.generateKeyPair()
        miner.node.keyPair = AccountUtils.generateKeyPair()


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

        chat1.handleCommand("transaction ${HashUtils.bytesToHex(miner.node.keyPair!!.public.encoded)} hello")


        runBlocking {
            suspendCoroutine<Unit>{ r->
                (miner.node as BaseMiner).onMined.add{
                    println("I SHOULD BE RESUMED!!!!!!!!!!??")
                    r.resume(Unit)
                }
            }

            (miner.node as BaseMiner).onMined.clear()


            assertEquals(
                    miner.node.chain.produce(BaseJSONSerializer()),
                    chat1.node.chain.produce(BaseJSONSerializer())
            )

            assertEquals(
                    miner.node.chain.produce(BaseJSONSerializer()),
                    chat2.node.chain.produce(BaseJSONSerializer())
            )

            assertEquals(
                    2,
                    miner.node.chain.blocks.size
            )

            assertEquals(
                    2,
                    chat1.node.chain.blocks.size
            )
            assertEquals(
                    2,
                    chat2.node.chain.blocks.size
            )


            val(_, _, result) =  "http://localhost:${chat1.http.port()}/messages".httpGet().responseString()
            when(result){
                is Result.Success -> {
                    val map: HashMap<String, Any> = jacksonObjectMapper().readValue(result.get(), object : TypeReference<Map<String, Any>>(){})

                    val transactions = (map["messages"] as List<BaseTransactionModel>)
                    assertEquals(
                            1,
                            transactions.size
                    )
                }
                else -> {
                    println("Fuckin ghsit")
                }
            }
        }

        chat1.handleCommand("transaction ${HashUtils.bytesToHex(chat2.node.keyPair!!.public.encoded)} hellothere")

        runBlocking {
            println("Suspending coroutine i guess")
            val latch = CountDownLatch(1)
            (miner.node as BaseMiner).onMined.add{
                latch.countDown()
            }
            latch.await()
//
//
            assertEquals(
                    miner.node.chain.produce(BaseJSONSerializer()),
                    chat1.node.chain.produce(BaseJSONSerializer())
            )

            assertEquals(
                    miner.node.chain.produce(BaseJSONSerializer()),
                    chat2.node.chain.produce(BaseJSONSerializer())
            )

            assertEquals(
                    3,
                    miner.node.chain.blocks.size
            )

            assertEquals(
                    3,
                    chat1.node.chain.blocks.size
            )
            assertEquals(
                    3,
                    chat2.node.chain.blocks.size
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
        }

    }

    @Test
    fun `Test full client`(){
        val chat1 = Chat()
        chat1.setAccount(AccountUtils.generateKeyPair())

        val chat2 = Chat()
        chat2.setAccount(AccountUtils.generateKeyPair())


        val miner = Chat(true)

        async { (miner.node as BaseMiner).startMineLoop() }

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

        val latch = CountDownLatch(1)
        (miner.node as BaseMiner).onMined.add{
            latch.countDown()
        }
        latch.await()

        assertEquals(
                2,
                miner.node.chain.blocks.size
        )

        assertEquals(
                2,
                chat1.node.chain.blocks.size
        )

        assertEquals(
                2,
                chat2.node.chain.blocks.size
        )


        runBlocking{
            val(_, _, result) =  "http://localhost:${chat1.http.port()}/messages".httpGet().responseString()
            val map: HashMap<String, Any> = jacksonObjectMapper().readValue(result.get(), object : TypeReference<Map<String, Any>>(){})

            val transactions = (map["messages"] as List<BaseTransactionModel>)
            assertEquals(
                    1,
                    transactions.size
            )
        }

        runBlocking{
            val(_, _, result) =  "http://localhost:${chat2.http.port()}/messages".httpGet().responseString()
            val map: HashMap<String, Any> = jacksonObjectMapper().readValue(result.get(), object : TypeReference<Map<String, Any>>(){})

            val transactions = (map["messages"] as List<BaseTransactionModel>)
            assertEquals(
                    1,
                    transactions.size
            )
        }


    }
}