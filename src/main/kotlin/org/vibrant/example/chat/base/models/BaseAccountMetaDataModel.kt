package org.vibrant.example.chat.base.models

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonTypeName("account-meta-data")
data class BaseAccountMetaDataModel(val name: String, override val timestamp: Long): TransactionPayload(timestamp)