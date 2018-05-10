package com.messagingservice.mq.rabbitmq

import com.google.gson.GsonBuilder
import com.messagingservice.domain.MessageAudit
import com.messagingservice.domain.MessageAuditDeserializer
import com.messagingservice.log.MessageAuditLogger
import com.rabbitmq.client.*
import java.io.IOException
import org.apache.logging.log4j.LogManager
import java.nio.charset.Charset

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 13-Feb-18.
 */

class RabbitMessageAuditListener (auditQueue: String, var brokerConnection: RabbitBrokerConnection, val messageAuditConnectionClosedListener: IMessageAuditConnectionClosedListener) {

    private var consumer: DefaultConsumer
    private var auditQueueName: String

    private lateinit var messageAuditLogger: MessageAuditLogger

    init {
        this.consumer = createAuditConsumer()
        this.auditQueueName = auditQueue
    }

    fun listenForAuditEvents(messageAuditLogger: MessageAuditLogger) {
        this.messageAuditLogger = messageAuditLogger

        brokerConnection.channel.basicConsume(this.auditQueueName, true, this.consumer)
    }

    private fun createAuditConsumer(): DefaultConsumer {
        return object : DefaultConsumer(brokerConnection.channel) {
            @Throws(IOException::class)
            override fun handleDelivery(consumerTag: String, envelope: Envelope,
                                        properties: AMQP.BasicProperties, body: ByteArray) {

                val message = String(body, Charset.defaultCharset())

                val gson = GsonBuilder()
                        .registerTypeAdapter(MessageAudit::class.java, MessageAuditDeserializer())
                        .create()

                val log = gson.fromJson(
                        message,
                        MessageAudit::class.java
                )

                messageAuditLogger.logMessageAudit(log)

                logger.info("Adding log to the queue before saving it to DB: $message")
            }

            override fun handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException) {
                if (sig.isInitiatedByApplication) {
                    logger.error("The connection to the broker with message audit log queue was shut down. -" /*+ id(consumerTag)*/)

                } else if (sig.reference is Channel) {
                    val nb = (sig.reference as Channel).channelNumber
                    logger.error("Message Audit Consumer ${brokerConnection.broker.name} was shut down. Channel #$nb " + consumerTag)

                } else {
                    logger.error("Message Audit Consumer ${brokerConnection.broker.name} was shut down - " + consumerTag)
                }

                messageAuditConnectionClosedListener.messageAuditConnectionClosed()
            }
        }
    }

    fun reconnectToBroker(activeBroker: RabbitBrokerConnection) {
        this.brokerConnection = activeBroker

        this.brokerConnection.channel.basicConsume(this.auditQueueName, true, this.consumer)
    }

    companion object {
        private var logger = LogManager.getLogger(RabbitMessageAuditListener::class.java.name)
    }
}