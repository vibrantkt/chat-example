package org.vibrant.example.chat.base.models

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import org.vibrant.core.models.block.ClassicBlockModel

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonTypeName("block")
data class BaseBlockModel(
        override val index: Long,
        override val hash: String,
        override val previousHash: String,
        val timestamp: Long,
        val transactions: List<BaseTransactionModel>,
        val nonce: Long
): ClassicBlockModel(index, hash, previousHash)