package org.vibrant.example.chat.base.models

import org.vibrant.core.models.Model


data class EchoHelloContract(val author: String, val address: String, val triggerWord: String, val response: String): Model()