package com.messagingservice.utils

import org.json.JSONObject
import java.util.*

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 04-Mar-18.
 */

class JsonObjectBuilder {
    private val deque: Deque<JSONObject> = ArrayDeque()

    fun json(build: JsonObjectBuilder.() -> Unit): JSONObject {
        deque.push(JSONObject())
        this.build()
        return deque.pop()
    }

    infix fun <T> String.To(value: T) {
        deque.peek().put(this, value)
    }
}