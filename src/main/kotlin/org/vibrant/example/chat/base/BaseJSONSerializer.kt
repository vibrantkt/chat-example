package org.vibrant.example.chat.base

import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


import org.vibrant.example.chat.base.models.BaseBlockChainModel
import org.vibrant.example.chat.base.models.BaseBlockModel
import org.vibrant.example.chat.base.models.BaseTransactionModel
import org.vibrant.core.models.Model
import org.vibrant.core.ModelSerializer
import org.vibrant.core.rpc.json.JSONRPCEntity
import org.vibrant.core.rpc.json.JSONRPCRequest
import org.vibrant.core.rpc.json.JSONRPCResponse
import org.vibrant.example.chat.base.models.AbstractBaseModel
import java.util.HashMap




object BaseJSONSerializer : ModelSerializer(){
    override fun deserialize(serialized: ByteArray): Model {
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
                throw Exception("Can't deserialize type ${map["@type"]}")
            }
        }

        return jacksonObjectMapper().readValue(serialized, targetType)
    }

    override fun serialize(model: Model): ByteArray {
        return jacksonObjectMapper().writeValueAsBytes(model)
    }


    fun serializeToString(model: Model): String {
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

    fun deserializeJSONRPC(serialized: ByteArray): JSONRPCEntity {
        return this.deserializeJSONRPC(String(serialized))
    }

}