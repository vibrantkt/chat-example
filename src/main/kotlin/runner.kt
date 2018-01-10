import kotlinx.coroutines.experimental.async
import org.vibrant.example.chat.base.Chat
import org.vibrant.example.chat.base.node.BaseMiner
import org.vibrant.example.chat.base.util.AccountUtils


fun main(args: Array<String>) {
    val chat = Chat()
    chat.setAccount(AccountUtils.generateKeyPair())

    val chat2 = Chat()
    chat2.setAccount(AccountUtils.generateKeyPair())


    val miner = Chat(true)

    async { (miner.node as BaseMiner).startMineLoop() }


}