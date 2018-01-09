package org.vibrant.example.chat.base.jsonrpc

data class JSONRPCResponse<T>(
        val result: T?,
        val error: Exception?,
        val id: Long,
        val version: String = "2.0"
): JSONRPCEntity()