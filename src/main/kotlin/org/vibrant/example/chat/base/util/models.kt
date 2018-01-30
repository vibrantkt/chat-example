package org.vibrant.example.chat.base.util


import org.vibrant.core.ConcreteModelSerializer
import org.vibrant.core.models.Model
import org.vibrant.core.rpc.json.JSONRPCResponse
import org.vibrant.example.chat.base.BaseJSONSerializer

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


fun <T: Model>serializerFor(): ConcreteModelSerializer<T> {
    return object: ConcreteModelSerializer<T>() {
        @Suppress("UNCHECKED_CAST")
        override fun deserialize(serialized: ByteArray): T {
            return BaseJSONSerializer.deserialize(serialized) as T
        }

        override fun serialize(model: Model): ByteArray {
            return BaseJSONSerializer.serialize(model)
        }

    }
}