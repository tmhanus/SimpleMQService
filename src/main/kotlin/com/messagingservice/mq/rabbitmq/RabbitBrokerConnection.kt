package com.messagingservice.mq.rabbitmq

import com.messagingservice.domain.Broker
import com.messagingservice.mq.rabbitmq.admin.AdminConnector
import com.messagingservice.utils.EncryptionProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 13-Mar-18.
 */

class RabbitBrokerConnection (var broker: Broker, properties: EncryptionProperties) {
    var channel: Channel
    var admin: AdminConnector

    init {
        val factory = ConnectionFactory()

        factory.host = broker.ip
        factory.port = broker.port

        channel = factory.newConnection().createChannel()
        admin = AdminConnector(broker, properties)
    }
}