package com.messagingservice.mq.rabbitmq.admin

import com.google.gson.annotations.SerializedName

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 03-Mar-18.
 */

// -------------- EXCHANGE -------------------

data class Exchange(
    var name: String,
    var type: ExchangeTypes,
    var durable: Boolean) {

    companion object Factory {
        fun getExchangeType(type: String?): ExchangeTypes {
            return when(type) {
                "fanout" -> ExchangeTypes.FANOUT
                else -> ExchangeTypes.DEFAULT
            }
        }
    }
}

// -------------- BINDING -------------------

data class Binding(
    var source: String,
    var vhost: String,
    var destination: String,
    var destinationType: String
)

// -------------- ADMIN MESSAGE -------------------


data class AdminMessage(
        val payload: String,
        @SerializedName("payload_bytes")
        val payloadBytes: Int,
        val exchange: String
)

// -------------- ADMIN QUEUE -------------------

data class AdminQueue(
    val name: String,
    val consumersNumber: Int = 0
)

// -------------- ADMIN ERROR -------------------

data class AdminError(val error: String, val reason: String)

// -------------- EXCHANGE TYPEs -------------------
enum class ExchangeTypes {
    DEFAULT, FANOUT
}