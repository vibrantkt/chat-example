package org.vibrant.example.chat.base.models

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import org.vibrant.base.database.blockchain.models.BlockChainModel

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonTypeName("blockchain")
data class BaseBlockChainModel(
        val difficulty: Int,
        val blocks: List<BaseBlockModel>
): BlockChainModel()