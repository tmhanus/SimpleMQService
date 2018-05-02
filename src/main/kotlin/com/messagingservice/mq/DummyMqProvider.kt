package com.messagingservice.mq

import com.messagingservice.domain.*
import com.messagingservice.log.MessageAuditLogger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 01-May-18.
 */

@Component
@ConditionalOnProperty(name = ["messageQueueProvider"], havingValue = "dummy")
class DummyMqProvider: IMqProvider {
    override fun purgeTopic(topicName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun purgeQueue(queueName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTopics(): List<Topic> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createTopic(topicName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteTopic(topicName: String, forceDelete: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subscribeTopic(topicName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unsubscribeTopic(topicName: String, deleteIfLast: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readFromTopic(topicName: String): Message? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun publishTopic(message: String, topicName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getQueues(): List<Queue> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createQueue(queueName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteQueue(queueName: String, forceDelete: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subscribeQueue(queueName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unsubscribeQueue(queueName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun publishQueue(message: String, queueName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readFromQueue(queueName: String): Message? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unsubscribeAllQueues(user: AuthUserDetails) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setNextAvailableBroker() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun reviveDeadBrokers() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getActiveBrokersNumber(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setupPolicies(retentionHours: Int, replicationLevel: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun registerMessageLogQueue(queueName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun registerMessageAuditLogListener(queueName: String, messageAuditLogger: MessageAuditLogger) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun logMessageAudit(msgAudit: MessageAudit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}