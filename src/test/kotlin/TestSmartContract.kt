import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.vibrant.core.database.blockchain.BlockChain
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.Chat
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseMessageModel
import org.vibrant.example.chat.base.models.EchoHelloContract
import org.vibrant.example.chat.base.util.AccountUtils
import kotlin.coroutines.experimental.suspendCoroutine

class TestSmartContract {


    private fun waitBlock(chat: Chat, predicate: (BaseBlockModel) -> Boolean){
        runBlocking {
            suspendCoroutine<Unit> { c ->
                chat.node.chain.addNewBlockListener(object: BlockChain.NewBlockListener<BaseBlockModel>{
                    override fun nextBlock(blockModel: BaseBlockModel) {
                        if(predicate(blockModel)) {
                            try {
                                c.resume(Unit)
                            } catch (e: Exception) {
                            }
                        }
                    }
                })
            }
        }
    }


    @Test
    fun `Test deploy of smart contract`(){
        val chat1 = Chat()
        chat1.setAccount(AccountUtils.generateKeyPair())

        val miner = Chat(isMiner = true)


        chat1.handleCommand("connect localhost:${miner.node.peer.port}")

        val contract = EchoHelloContract(chat1.hexAddress(),
                "address1",
                "hello",
                "Hello, its automatic response!")

        chat1.node.deployContract(contract, chat1.keyPair, true)

        chat1.waitBlock { true }

        assertEquals(
                2,
                chat1.node.chain.blocks().size
        )

        chat1.node.message(chat1.hexAddress(), "hello", chat1.keyPair, false)


        waitBlock(chat1, { block ->
            block.transactions.findLast { transaction ->
                when(transaction.payload){
                    is BaseMessageModel -> {
                        (transaction.payload as BaseMessageModel).content == contract.response
                    }
                    else -> false
                }
            } != null
        })

    }
}