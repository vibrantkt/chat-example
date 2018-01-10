package org.vibrant.example.chat.base.models

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import org.vibrant.core.models.Model

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(*arrayOf(
    JsonSubTypes.Type(value = BaseMessageModel::class, name = "message"),
    JsonSubTypes.Type(value = BaseAccountMetaDataModel::class, name = "account-meta-data")
))
abstract class TransactionPayload(open val timestamp: Long): Model()