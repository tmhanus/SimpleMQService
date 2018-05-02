package com.messagingservice.mq

import com.messagingservice.domain.*
import com.messagingservice.log.MessageAuditLogger

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 25-Feb-18.
 */
interface IMqProvider {
    // TOPIC
    fun getTopics() : List<Topic>

    fun createTopic(topicName: String)

    fun deleteTopic(topicName: String, forceDelete: Boolean)

    fun purgeTopic(topicName: String)

    fun subscribeTopic(topicName: String)

    fun unsubscribeTopic(topicName: String, deleteIfLast: Boolean)

    fun readFromTopic(topicName: String): Message?

    fun publishTopic(message: String, topicName: String)

    // QUEUE
    fun getQueues(): List<Queue>

    fun createQueue(queueName: String)

    fun deleteQueue(queueName: String, forceDelete: Boolean)

    fun purgeQueue(queueName: String)

    fun subscribeQueue(queueName: String)

    fun unsubscribeQueue(queueName: String)

    fun publishQueue(message: String, queueName: String)

    fun readFromQueue(queueName: String): Message?

    fun unsubscribeAllQueues(user: AuthUserDetails)

    // AVAILABILITY
    fun setNextAvailableBroker()

    fun reviveDeadBrokers()

    fun getActiveBrokersNumber(): Int

    fun setupPolicies(retentionHours: Int, replicationLevel: Int)

    fun registerMessageLogQueue(queueName: String)

    fun registerMessageAuditLogListener(queueName: String, messageAuditLogger: MessageAuditLogger)

    fun logMessageAudit(msgAudit: MessageAudit)
}