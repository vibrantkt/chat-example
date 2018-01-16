package org.vibrant.example.chat.base.models

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import org.vibrant.base.database.blockchain.models.BlockModel

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonTypeName("block")
data class BaseBlockModel(
        val index: Long,
        val hash: String,
        val prevHash: String,
        val timestamp: Long,
        val transactions: List<BaseTransactionModel>,
        val nonce: Long
): BlockModel()