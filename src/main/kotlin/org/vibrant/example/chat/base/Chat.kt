package org.vibrant.example.chat.base

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.Javalin
import io.javalin.embeddedserver.Location
import io.javalin.embeddedserver.jetty.websocket.WsSession
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import org.vibrant.core.models.BlockModel
import org.vibrant.core.node.RemoteNode
import org.vibrant.core.producers.BlockChainProducer
import org.vibrant.example.chat.base.models.BaseAccountMetaDataModel
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.node.BaseMiner
import org.vibrant.example.chat.base.node.BaseNode
import org.vibrant.example.chat.base.node.HTTPPeer
import org.vibrant.example.chat.base.producers.BaseBlockChainProducer
import org.vibrant.example.chat.base.util.AccountUtils
import org.vibrant.example.chat.base.util.HashUtils
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyPair
import java.util.*

class Chat(private val isMiner: Boolean = false){

    private val logger = KotlinLogging.logger {  }

    val node = createNode()

    internal var keyPair = AccountUtils.generateKeyPair()


    internal val listeners = arrayListOf<WsSession>()

    val http = Javalin.create().ws("/event"){ ws ->
        ws.onConnect { session -> this@Chat.listeners.add(session) }
        ws.onMessage { _, message ->
            logger.info { "Got message from ws client $message" }
        }
        ws.onClose { session, _, _ -> this@Chat.listeners.remove(session) }
        ws.onError { session, _ -> this@Chat.listeners.remove(session) }
    }.get("blockchain"){ ctx ->
        val response = jacksonObjectMapper().writeValueAsString(node.chain.produce(BaseJSONSerializer))
        ctx.result(response)
    }.get("account"){ ctx ->
        val map = hashMapOf<String, Any>()
        map["public"] = this@Chat.hexAddress()
        map["peers"] = this.node.peer.peers
        map["miners"] = this.node.peer.miners
        val response = jacksonObjectMapper().writeValueAsString(map)
        ctx.result(response)
    }.post("command"){ ctx ->
        val map: HashMap<String, Any> = jacksonObjectMapper().readValue(ctx.body(), object : TypeReference<Map<String, Any>>(){})
        this@Chat.handleCommand(map["command"].toString())
        ctx.result("true")
    }.enableStaticFiles(
            Chat::class.java.classLoader.getResource("html").toString().substring(5),
            Location.EXTERNAL
    ).port(node.peer.port + 1000).start()

    internal fun hexAddress(): String{
        return HashUtils.bytesToHex(keyPair.public.encoded!!)
    }

    init {
        this.node.start()
        this.node.chain.addNewBlockListener(object : BaseBlockChainProducer.NewBlockListener() {
            override fun nextBlock(blockModel: BaseBlockModel) {
                this@Chat.listeners.forEach{
                    val response = jacksonObjectMapper().writeValueAsString(blockModel)
                    logger.info { response }
                    it.send(response)
                }
            }
        })

    }

    fun setAccount(keyPair: KeyPair){
        this.keyPair = keyPair
        logger.info { "Account: ${this@Chat.hexAddress()}" }
    }

    fun handleCommand(str: String){
        val (command, parameters) = str.split(Regex(" "), 2)
        when(command){
            "connect" -> {
                val d = parameters.split(":")
                logger.info { "Connecting to ${d[0] + d[1].toInt()}" }
                node.connect(RemoteNode(d[0], d[1].toInt()))
                logger.info { "Connected to ${d[0] + d[1].toInt()}" }
            }
            "auth" -> {
                val keyFile = File(parameters)
                logger.info { "Getting key from $parameters" }
                this.keyPair = AccountUtils.deserializeKeyPair(keyFile.readBytes())
                logger.info { "Authed." }
            }
            "transaction" -> {
                val (address, payload) = parameters.split(Regex(" "), 2)
                this@Chat.message(address, payload)
            }
            "account" -> {
                this@Chat.changeName(parameters)
                logger.info { "Name changed i guess" }
            }
            else -> {
                logger.info { "Unrecognized command" }
            }
        }
    }

    private fun changeName(name: String) {
        val response = this.node.transaction(this@Chat.hexAddress(), BaseAccountMetaDataModel(name, Date().time), keyPair)
        logger.info { "Transaction broadcasted(change name) $response" }
    }


    private fun createNode(): BaseNode {
        var port = 7000
        while(true){
            port++
            try {
                Socket("localhost", port).close()
            }catch (e: Exception){
                return if(isMiner) BaseMiner(port) else BaseNode(port)
            }
        }
    }


    fun message(hexAddressTo: String, message: String){
        val response = this.node.transaction(hexAddressTo, message, keyPair)
        logger.info { "Transaction broadcasted (message) $response" }
    }

    fun stop(){
        this.http.stop()
    }
}