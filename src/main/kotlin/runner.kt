import kotlinx.coroutines.experimental.async
import org.vibrant.example.chat.base.Chat
import org.vibrant.example.chat.base.node.BaseMiner
import org.vibrant.example.chat.base.util.AccountUtils


fun main(args: Array<String>) {


    val isMiner = readLine() == "true"

    val client = Chat(isMiner)
    if (!isMiner)
        client.setAccount(AccountUtils.generateKeyPair())
}