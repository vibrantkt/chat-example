@file:Suppress("ArrayInDataClass")

package org.vibrant.example.chat.base.models

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import org.vibrant.base.database.blockchain.models.TransactionModel

/***
 * Model of transaction which is serializable
 * @property from address of sender
 * @property to address of receiver
 * @property payload any content
 * @property signature signature
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonTypeName("transaction")
data class BaseTransactionModel(
        val from: String,
        val to: String,
        val payload: TransactionPayload,
        val signature: String
): TransactionModel()