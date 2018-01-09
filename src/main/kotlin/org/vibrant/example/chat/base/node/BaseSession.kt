package org.vibrant.example.chat.base.node

import org.vibrant.example.chat.base.jsonrpc.JSONRPCEntity
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse
import org.vibrant.core.node.RemoteNode

abstract class BaseSession(val remoteNode: RemoteNode, val request: JSONRPCRequest){
    abstract fun handle(response: JSONRPCResponse<*>)
}