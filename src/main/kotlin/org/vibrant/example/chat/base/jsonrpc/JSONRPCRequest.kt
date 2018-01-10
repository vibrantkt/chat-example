package org.vibrant.example.chat.base.jsonrpc

import org.vibrant.example.chat.base.BaseJSONSerializer
import java.util.*


data class JSONRPCRequest(
        val method: String,
        val params: Array<Any>,
        override val id: Long,
        val version: String = "2.0"
): JSONRPCEntity(id) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JSONRPCRequest

        if (method != other.method) return false
        if (!Arrays.equals(params, other.params)) return false
        if (id != other.id) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + Arrays.hashCode(params)
        result = 31 * result + id.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }
}