package com.messagingservice.domain

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 15-Apr-18.
 */

class MessageAuditDeserializer: JsonDeserializer<MessageAudit> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): MessageAudit{
        return  Gson().fromJson(json?.asJsonObject, MessageAudit::class.java)
    }
}