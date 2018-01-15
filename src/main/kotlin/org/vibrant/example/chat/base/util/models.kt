package org.vibrant.example.chat.base.util

import org.vibrant.core.models.Model
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse

fun JSONRPCResponse<*>.stringResult(): String {
    return this.result.toString()
}

fun <T: Model>JSONRPCResponse<*>.deserialize(): T {
    @Suppress("UNCHECKED_CAST")
    return BaseJSONSerializer.deserialize(this.result.toString().toByteArray()) as T
}



fun Model.serialize(): String {
    return String(BaseJSONSerializer.serialize(this))
}