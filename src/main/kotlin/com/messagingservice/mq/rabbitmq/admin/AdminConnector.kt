package com.messagingservice.mq.rabbitmq.admin

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.messagingservice.domain.Broker
import com.messagingservice.mq.rabbitmq.serialization.ExchangeDeserializer
import com.messagingservice.service.ResourceNotFound
import com.messagingservice.utils.EncryptionProperties
import com.messagingservice.utils.JsonObjectBuilder
import com.messagingservice.utils.MqEncryptor
import org.json.JSONObject
import java.net.HttpURLConnection

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 03-Mar-18.
 */
class AdminConnector (private val broker: Broker, properties: EncryptionProperties){
    private val defaultVHost = "%2F"

    private val password = MqEncryptor.decrypt(this.broker.adminPassword, properties.aes.iv, properties.aes.key)

    private val managementUrl: String = "http://${this.broker.adminLogin}:" +
            "${this.password}@${this.broker.ip}:${this.broker.adminPort}"

    /**
     * Returns list of queues. Calls management API to get this list.
     */
    fun getQueues(vHost: String = defaultVHost): MutableList<AdminQueue> {
        val request = createGetRequest("/api/queues/$vHost/")

        val (_, response, result) =  request.responseString()

        var queues = mutableListOf<AdminQueue>()

        if (response.statusCode == HttpURLConnection.HTTP_OK) {
            queues = Gson().fromJson(
                    result.component1().toString(),
                    object : TypeToken<List<AdminQueue>>() {}.type
            )
        }
        return queues
    }

    fun getQueue(queueName: String, vHost: String = defaultVHost): AdminQueue? {
        val request = createGetRequest("/api/queues/$vHost/$queueName/")

        val (_, response, result) =  request.responseString()

        if (response.statusCode == HttpURLConnection.HTTP_OK) {
            return Gson().fromJson(
                    result.component1().toString(),
                    AdminQueue::class.java)

        }
        return null
    }

    /**
     * Returns list of exchanges. Calls management API to get this list.
     */
    fun getExchanges(vHost: String = defaultVHost): MutableList<Exchange> {
        val request = createGetRequest("/api/exchanges/$vHost/")

        val (_, response, result) =  request.responseString()

        var exchanges = mutableListOf<Exchange>()

        if (response.statusCode == HttpURLConnection.HTTP_OK) {
            val gson = GsonBuilder()
                    .registerTypeAdapter(Exchange::class.java, ExchangeDeserializer())
                    .create()

            exchanges = gson.fromJson(
                    result.component1().toString(),
                    object : TypeToken<List<Exchange>>() {}.type
            )
        }

        return exchanges
    }

    fun getExchange(exchangeName: String, vHost: String = defaultVHost): Exchange? {
        val request = createGetRequest("/api/exchanges/$vHost/$exchangeName/")

        val (_, response, result) =  request.responseString()

        if (response.statusCode == HttpURLConnection.HTTP_OK) {
            val gson = GsonBuilder()
                    .registerTypeAdapter(Exchange::class.java, ExchangeDeserializer())
                    .create()

            return gson.fromJson(
                    result.component1().toString(),
                    Exchange::class.java)
        }

        return null
    }

    /**
     * Returns list of bindings for a specific queue. Calls management API to get this list.
     */
    fun getBindings(vHost: String = defaultVHost, queueName: String): MutableList<Binding> {
        val request = createGetRequest("/api/queues/$vHost/$queueName/bindings")

        val (_, response, result) =  request.responseString()

        var bindings = mutableListOf<Binding>()

        if (response.statusCode == HttpURLConnection.HTTP_OK) {
            bindings = Gson().fromJson(
                    result.component1().toString(),
                    object : TypeToken<List<Binding>>() {}.type
            )
        }

        return bindings
    }

    /**
     * Set replication level using policies.
     */
    fun changeReplicationLevel(replicationLevel: Int, policyName: String, vhost: String = defaultVHost) {
        val definition = json {
            "ha-mode" To "exactly"
            "ha-params" To replicationLevel
            "ha-sync-mode" To "automatic"
        }

        val requestBody = json {
            "pattern" To "^.*"
            "definition" To definition
            "apply-to" To "queues"
        }

        val request = createPutRequest("/api/policies/$vhost/$policyName", requestBody.toString())

        val (_, response, result) =  request.responseString()

        if (response.statusCode != HttpURLConnection.HTTP_CREATED && response.statusCode != HttpURLConnection.HTTP_NO_CONTENT) {
            //TODO log internal error!!!
            throw InternalError("Something wen't wrong. Contact your admin.")
        }
    }

    fun isNodeHealthy(): Boolean {
        val request = createGetRequest("/api/healthchecks/node")

        val (_, response, result) =  request.responseString()

        if (response.statusCode == HttpURLConnection.HTTP_OK) {
            return true
        }

        // todo reason???
        return false
    }

    fun getMessage(queueName: String, vhost: String = defaultVHost): AdminMessage? {

        val requestBody = json {
            "count" To 1
            "ackmode" To "ack_requeue_true"
            "encoding" To "auto"
        }

        val request = createPostRequest("/api/queues/$vhost/$queueName/get", requestBody.toString())

        val (_, response, result) =  request.responseString()

        if (response.statusCode == HttpURLConnection.HTTP_OK) {
            val gson = Gson()

            val messages: List<AdminMessage> = gson.fromJson(
                    result.component1().toString(),
                    object : TypeToken<List<AdminMessage>>() {}.type
            )

            return messages.firstOrNull()
        } else {
            throw ResourceNotFound("Queue $queueName was not found.")
            //todo check unauthorized queue
        }
    }

    /**
     * Expects a specific api request url and then creates a GET request for current broker.
     * It also sets up an authentication for request.
     */
    private fun createGetRequest(apiUrl: String): Request {
        val url = this.managementUrl + apiUrl

        return url.httpGet().authenticate(
                this.broker.adminLogin,
                this.password
        )
    }

    /**
     * Expects a specific api request url and then creates a POST request for current broker.
     * It also sets up an authentication for request and appends a body to the request.
     */
    private fun createPostRequest(apiUrl: String, body: String): Request {
        val url = this.managementUrl + apiUrl
        val request = url.httpPost().body(body).authenticate(
                this.broker.adminLogin,
                this.password)

        request.headers["Content-Type"] = "application/json"

        return request
    }

    /**
     * Expects a specific api request url and then creates a PUT request for current broker.
     * It also sets up an authentication for request and appends a body to the request.
     */
    private fun createPutRequest(apiUrl: String, body: String): Request {
        val url = this.managementUrl + apiUrl
        val request = url.httpPut().body(body).authenticate(
                this.broker.adminLogin,
                this.password)

        request.headers["Content-Type"] = "application/json"

        return request
    }

    fun json(build: JsonObjectBuilder.() -> Unit): JSONObject {
        return JsonObjectBuilder().json(build)
    }
}