import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.models.BaseMessageModel
import org.vibrant.example.chat.base.node.BaseMiner
import org.vibrant.example.chat.base.node.BaseNode
import org.vibrant.example.chat.base.producers.BaseTransactionProducer
import org.vibrant.core.node.RemoteNode
import org.vibrant.core.reducers.SignatureProducer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse
import org.vibrant.example.chat.base.util.AccountUtils
import org.vibrant.example.chat.base.util.HashUtils
import java.security.KeyPair
import kotlin.coroutines.experimental.suspendCoroutine

class TestBasePeer {


    @Test
    fun `Test peer echo`(){
        val node = BaseNode(7000)
        val miner = BaseMiner(7001)


        node.start()
        miner.start()

        val some = node.connect(RemoteNode("localhost", 7001))
        // check ping - pong
        assertEquals(
                true,
                some
        )

        // connection established

        assertEquals(
                node.peer.peers.size,
                1
        )

        assertEquals(
                node.peer.miners.size,
                1
        )

        assertEquals(
                miner.peer.peers.size,
                1
        )

        //cuz node isn't a
        assertEquals(
                miner.peer.miners.size,
                0
        )


        node.stop()
        miner.stop()
    }


    @Test
    fun `Test peer return type`(){
        val node = BaseNode(7000)
        val miner = BaseMiner(7001)


        node.start()
        miner.start()


        val expectedNode = node.rpc.nodeType(JSONRPCRequest("a", arrayOf(),1L), RemoteNode("", 0))
        val expectedMiner = miner.rpc.nodeType(JSONRPCRequest("a", arrayOf(),1L), RemoteNode("", 0))

        assertEquals(
                "node",
                expectedNode.result.toString()
        )

        assertEquals(
                "miner",
                expectedMiner.result.toString()
        )

        node.stop()
        miner.stop()
    }


    @Test
    fun `Test peer behind sync`(){
        val node = BaseNode(7000)
        val miner = BaseMiner(7001)


        node.start()
        miner.start()

        miner.chain.pushBlock(node.chain.createBlock(
                listOf(),
                BaseJSONSerializer()
        ))


        node.connect(RemoteNode("localhost", 7001))
        node.synchronize(RemoteNode("localhost", 7001))


        assertEquals(
                miner.chain.produce(BaseJSONSerializer()),
                node.chain.produce(BaseJSONSerializer())
        )

        miner.stop()
        node.stop()
    }


    @Test
    fun `Test peer ahead sync`(){

        val sender = AccountUtils.generateKeyPair()

        val transaction = BaseTransactionProducer(
                "yura",
                "vasya",
                BaseMessageModel("Hello!", 0),
                sender,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer())

        val node = BaseNode(7000)
        val miner = BaseMiner(7001)

        node.start()
        miner.start()

        miner.chain.pushBlock(miner.chain.createBlock(
                listOf(transaction),
                BaseJSONSerializer()
        ))

        miner.connect(RemoteNode("localhost", 7000))

        val change = async {
            suspendCoroutine<Unit> { s ->
                node.chain.onChange.add { _ ->
                    s.resume(Unit)
                }
            }
        }

        miner.synchronize(RemoteNode("localhost", 7000))

        runBlocking {
            change.await()
        }

        assertEquals(
                miner.chain.produce(BaseJSONSerializer()),
                node.chain.produce(BaseJSONSerializer())
        )


        node.stop()
        miner.stop()


    }


    @Test
    fun `Test add transaction loop from node to miner`(){
        val miner = BaseMiner(7001)

        val sender = AccountUtils.generateKeyPair()

        val transaction = BaseTransactionProducer(
                "yura",
                "vasya",
                BaseMessageModel("Hello!", 0),
                sender,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer())

        miner.addTransaction(transaction)
        miner.mine()

        val chain = miner.chain.produce(BaseJSONSerializer())

        assertEquals(
                2,
                chain.blocks.size
        )

        assertEquals(
                "0",
                chain.blocks[1].hash.substring(0, miner.chain.difficulty)
        )

        miner.stop()


    }

    @Test
    fun `Test remote transaction handle`(){

        val sender = AccountUtils.generateKeyPair()

        val transaction = BaseTransactionProducer(
                "yura",
                "vasya",
                BaseMessageModel("Hello!", 0),
                sender,
                object : SignatureProducer {
                    override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
                        return HashUtils.signData(content, keyPair)
                    }
                }
        ).produce(BaseJSONSerializer())

        val node = BaseNode(7000)
        val miner = BaseMiner(7001)

        node.start()
        miner.start()


        miner.connect(RemoteNode("localhost", 7000))
        miner.synchronize(RemoteNode("localhost", 7000))


        runBlocking {
            val response = node.peer.request(RemoteNode("localhost", 7001), JSONRPCRequest(
                    method = "addTransaction",
                    params = arrayOf(BaseJSONSerializer().serialize(transaction)),
                    id = 5
            )) as JSONRPCResponse<*>


            assertEquals(
                    true,
                    response.result
            )

            suspendCoroutine<Unit>{ r->
                async {
                    miner.startMineLoop()
                }
                miner.onMined.add{
                    r.resume(Unit)
                }
            }

            // block is mined
            assertEquals(
                    2,
                    miner.chain.blocks.size
            )

            assertEquals(
                    2,
                    node.chain.blocks.size
            )

            assertEquals(
                    miner.chain.produce(BaseJSONSerializer()),
                    node.chain.produce(BaseJSONSerializer())
            )
        }


        node.stop()
        miner.stop()

    }



//
//    @Test
//    fun `Generate beautiful chain`(){
//        val chain = BaseBlockChainProducer(2)
//        for(i in 0.until(100)){
//            val sender = AccountUtils.generateKeyPair()
//            val transactions = (0.until(Random().nextInt(3))).map{
//                BaseTransactionProducer(
//                        "yura",
//                        "vasya",
//                        BaseMessageModel("Hello, my message is ${i}!", Date().time),
//                        sender,
//                        object : SignatureProducer {
//                            override fun produceSignature(content: ByteArray, keyPair: KeyPair): ByteArray {
//                                return HashUtils.signData(content, keyPair)
//                            }
//                        }
//                ).produce(BaseJSONSerializer())
//            }
//
//
//            chain.pushBlock(chain.createBlock(transactions, BaseJSONSerializer()))
//        }
//        println(BaseJSONSerializer().serialize(chain.produce(BaseJSONSerializer())))
//    }

}