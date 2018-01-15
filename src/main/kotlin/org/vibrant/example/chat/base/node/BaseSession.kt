package org.vibrant.example.chat.base.node

import org.vibrant.base.rpc.json.JSONRPCEntity
import org.vibrant.base.rpc.json.JSONRPCRequest

import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse
import org.vibrant.core.node.RemoteNode

abstract class BaseSession(val remoteNode: RemoteNode, val request: JSONRPCRequest){
    abstract fun handle(response: JSONRPCResponse<*>)
}