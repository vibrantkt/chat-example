package org.vibrant.example.chat.base.jsonrpc

import org.vibrant.core.models.Model
import org.vibrant.core.node.UDPSessionPeer
import org.vibrant.example.chat.base.BaseJSONSerializer

abstract class JSONRPCEntity(id: Long): UDPSessionPeer.Communication.CommunicationPackage(id){
    override fun toByteArray(): ByteArray {
        return BaseJSONSerializer().serialize(this).toByteArray(charset("UTF-8"))
    }
}