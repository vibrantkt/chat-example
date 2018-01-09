package org.vibrant.example.chat.base

import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.vibrant.example.chat.base.jsonrpc.JSONRPCEntity
import org.vibrant.example.chat.base.jsonrpc.JSONRPCRequest
import org.vibrant.example.chat.base.jsonrpc.JSONRPCResponse
import org.vibrant.example.chat.base.models.BaseBlockChainModel
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.core.models.Model
import org.vibrant.core.reducers.ModelSerializer
import java.util.HashMap




class BaseJSONSerializer : ModelSerializer(){
    override fun deserialize(serialized: String): Model {
        val map: HashMap<String, Any> = jacksonObjectMapper().readValue(serialized, object : TypeReference<Map<String, Any>>(){})

        val targetType = when(map["@type"]){
            BaseTransactionModel::class.java.getAnnotation(JsonTypeName::class.java).value -> {
                BaseTransactionModel::class.java
            }
            BaseBlockModel::class.java.getAnnotation(JsonTypeName::class.java).value -> {
                BaseBlockModel::class.java
            }
            BaseBlockChainModel::class.java.getAnnotation(JsonTypeName::class.java).value -> {
                BaseBlockChainModel::class.java
            }
            else -> {
                throw kotlin.Exception("Can't deserialize type ${map["@type"]}")
            }
        }

        return jacksonObjectMapper().readValue(serialized, targetType)
    }

    override fun <T : Model> serialize(model: T): String {
        return jacksonObjectMapper().writeValueAsString(model)
    }



    fun deserializeJSONRPC(serialized: String): JSONRPCEntity {
        val map: HashMap<String, Any> = jacksonObjectMapper().readValue(serialized, object : TypeReference<Map<String, Any>>(){})
        return if(map.containsKey("method")){
            jacksonObjectMapper().readValue(serialized, JSONRPCRequest::class.java)
        }else if(map.containsKey("result") || map.containsKey("error")){
            jacksonObjectMapper().readValue(serialized, JSONRPCResponse::class.java)
        }else{
            throw Exception("Unexpected json rpc entity")
        }
    }

}