import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.vibrant.base.rpc.json.JSONRPCRequest
import org.vibrant.core.algorithm.SignatureProducer
import org.vibrant.core.node.RemoteNode
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.models.BaseMessageModel
import org.vibrant.example.chat.base.node.BaseMiner
import org.vibrant.example.chat.base.node.Node
import org.vibrant.example.chat.base.producers.BaseTransactionProducer
import org.vibrant.example.chat.base.util.AccountUtils
import org.vibrant.example.chat.base.util.HashUtils
import org.vibrant.example.chat.base.util.serialize
import java.net.Socket
import java.security.KeyPair

class TestBasePeer {

    private fun createNode(isMiner: Boolean): Node {
        var port = 7000
        while(true){
            port++
            try {
                Socket("localhost", port).close()
            }catch (e: Exception){
                val node = if(isMiner) BaseMiner(port) else Node(port)
                node.start()
                return node
            }
        }
    }

    @Test
    fun `Test peer echo`(){

        val node = createNode(false)


        val miner = createNode(true) as BaseMiner


        node.start()
        miner.start()

        val some = node.connect(RemoteNode("localhost", miner.peer.port))
        assertEquals(
                true,
                some
        )

        // connection established

        assertEquals(
                1,
                node.peer.peers.size
        )

        assertEquals(
                1,
                node.peer.miners.size
        )

        assertEquals(
                1,
                miner.peer.peers.size
        )

        //cuz node isn't a mienr
        assertEquals(
                0,
                miner.peer.miners.size
        )


        node.stop()
        miner.stop()
    }


    @Test
    fun `Test peer return type`(){
        val node = createNode(false)
        val miner = createNode(true) as BaseMiner


        val expectedNode = node.rpc.nodeType(JSONRPCRequest("nodeType", arrayOf("node"),1L), RemoteNode("", 0))
        val expectedMiner = miner.rpc.nodeType(JSONRPCRequest("nodeType", arrayOf("node"),1L), RemoteNode("", 0))

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
        val node = createNode(false)
        val miner = createNode(true) as BaseMiner

        miner.chain.pushBlock(node.chain.createBlock(
                listOf(),
                BaseJSONSerializer
        ))


        runBlocking {

            node.connect(RemoteNode("localhost", miner.peer.port))
            node.synchronize(RemoteNode("localhost", miner.peer.port))
        }


        assertEquals(
                miner.chain.produce(BaseJSONSerializer),
                node.chain.produce(BaseJSONSerializer)
        )

        miner.stop()
        node.stop()
    }





    @Test
    fun `Test add transaction loop to miner`(){
        val miner = BaseMiner(7001)

        val sender = AccountUtils.generateKeyPair()

//        val thread = async(newSingleThreadContext("miner loop")) {
//            miner.startMineLoop()
//        }

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
        ).produce(BaseJSONSerializer)

        runBlocking {
            miner.addTransaction(transaction)
        }

        val chain = miner.chain.produce(BaseJSONSerializer)

        assertEquals(
                2,
                chain.blocks.size
        )

        assertEquals(
                "0".repeat(miner.chain.difficulty),
                chain.blocks[1].hash.substring(0, miner.chain.difficulty)
        )

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
        ).produce(BaseJSONSerializer)

        val node = Node(7000)
        val miner = BaseMiner(7001)

        node.start()
        miner.start()


        runBlocking {
            miner.connect(RemoteNode("localhost", 7000))
            miner.synchronize(RemoteNode("localhost", 7000))
        }

        runBlocking {
            val response = node.peer.request(RemoteNode("localhost", 7001), JSONRPCRequest(
                    method = "addTransaction",
                    params = arrayOf(transaction.serialize()),
                    id = 5
            ))


            assertEquals(
                    true,
                    response.result
            )

            assertEquals(
                    2,
                    miner.chain.blocks.size
            )

            assertEquals(
                    2,
                    node.chain.blocks.size
            )

            assertEquals(
                    miner.chain.produce(BaseJSONSerializer),
                    node.chain.produce(BaseJSONSerializer)
            )
        }

        node.stop()
        miner.stop()

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
        ).produce(BaseJSONSerializer)

        val node = Node(7000)
        val miner = BaseMiner(7001)

        node.start()
        miner.start()

        miner.chain.pushBlock(miner.chain.createBlock(
                listOf(transaction),
                BaseJSONSerializer
        ))

        miner.connect(RemoteNode("localhost", 7000))
        miner.synchronize(RemoteNode("localhost", 7000))

        assertEquals(
                miner.chain.produce(BaseJSONSerializer),
                node.chain.produce(BaseJSONSerializer)
        )

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
//                ).produce(BaseJSONSerializer)
//            }
//
//
//            chain.pushBlock(chain.createBlock(transactions, BaseJSONSerializer))
//        }
//        println(BaseJSONSerializer.serialize(chain.produce(BaseJSONSerializer)))
//    }

}