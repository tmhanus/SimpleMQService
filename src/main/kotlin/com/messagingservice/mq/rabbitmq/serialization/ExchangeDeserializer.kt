package com.messagingservice.mq.rabbitmq.serialization

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.messagingservice.mq.rabbitmq.admin.Exchange
import java.lang.reflect.Type

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 03-Mar-18.
 */

class ExchangeDeserializer : JsonDeserializer<Exchange> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Exchange {
        val exchange = Gson().fromJson(json?.asJsonObject, Exchange::class.java)

        exchange.type = Exchange.getExchangeType(json?.asJsonObject?.get("type")?.asString)

        return exchange
    }
}