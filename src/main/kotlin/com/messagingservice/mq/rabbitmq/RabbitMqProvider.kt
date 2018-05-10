package com.messagingservice.mq.rabbitmq

import com.google.gson.Gson
import com.messagingservice.domain.*
import com.messagingservice.domain.Queue
import com.messagingservice.log.MessageAuditLogger
import com.messagingservice.mq.IMqProvider
import com.messagingservice.mq.rabbitmq.admin.ExchangeTypes
import com.messagingservice.repository.BrokerRepository
import com.messagingservice.repository.SubscriptionRepository
import com.messagingservice.repository.UserRepository
import com.messagingservice.service.BadRequest
import com.messagingservice.service.MessagingService
import com.messagingservice.service.ResourceNotFound
import org.apache.logging.log4j.LogManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import com.messagingservice.utils.EncryptionProperties

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 25-Feb-18.
 */

@Component
@ConditionalOnProperty(name = ["messageQueueProvider"], havingValue = "rabbit", matchIfMissing = false)
class RabbitMqProvider(
        private val brokerRepository: BrokerRepository,
        private val subscriptionRepository: SubscriptionRepository,
        private val userRepository: UserRepository,
        private val encryptionProperties: EncryptionProperties)
    : IMqProvider,
        IMessageAuditConnectionClosedListener {

    private var brokerConnectors = mutableListOf<RabbitBrokerConnection>()
    private lateinit var activeBroker: RabbitBrokerConnection

    private lateinit var messageAuditQueue: String
    private lateinit var messageAuditListener: RabbitMessageAuditListener

    // random seed
    val random = Random()

    var retentionHours: Int = MessagingService.UNLIMITED_RETENTION
    var replicationLevel: Int = MessagingService.NO_REPLICATION
        set(value) {
            field = value
            activeBroker.admin.changeReplicationLevel(field, REPLICATION_POLICY_NAME)
        }

    init {
        log.info("Creating RabbitMqProvider")

        initBrokerConnections()

        this.activeBroker = getAvailableBrokerConnection()
    }

    private fun getAvailableBrokerConnection(): RabbitBrokerConnection {
        if (brokerConnectors.size == 0) {
            throw InternalError("There is no broker available. Pleas contact your administrator.")
        }

        if (brokerConnectors.size == 1) {
            return brokerConnectors.first()
        }

        val index = random.nextInt(brokerConnectors.size)

        val brokerConnection = brokerConnectors[index]

        if (brokerConnection.admin.isNodeHealthy()) {
            return brokerConnection
        } else {
            brokerConnection.broker.status = Broker.BROKER_OFFLINE
            brokerRepository.save(brokerConnection.broker)

            log.error("Broker ${brokerConnection.broker.name} on address ${brokerConnection.broker.ip} on " +
                    "port: ${brokerConnection.broker.port} isn't available. It is removed from active brokers.")

            brokerConnectors.removeAt(index)

            return getAvailableBrokerConnection() // recursively remove all inactive brokers
        }

    }

    override fun createTopic(topicName: String) {
        this.activeBroker.channel.exchangeDeclare(topicName, FANOUT_EXCHANGE)
    }

    override fun publishTopic(message: String, topicName: String) {
        if (checkIfExchangeExists(topicName)) {
            this.activeBroker.channel.basicPublish(topicName, "", null, message.toByteArray())
            logMessageAudit(MessageAudit(-1, getAuthenticatedUser().login, Date(), MessageDestinationType.TOPIC, topicName, message.length, MessageAudit.UPLOAD, activeBroker.broker.id))
        } else {
            throw ResourceNotFound("Topic $topicName does not exist.")
        }
    }

    override fun subscribeTopic(topicName: String) {
        val user = getAuthenticatedUser()

        if (subscriptionRepository.findByExchangeAndUser(topicName, user).firstOrNull() != null) {
            return
        }

        val arguments = mutableMapOf<String, Any>()

        arguments[QUEUE_TTL_ARGUMENT] = getRetentionValueInMilliseconds(retentionHours)

        val newQueueName = activeBroker.channel.queueDeclare("", true, false, false, arguments).queue

        if (checkIfExchangeExists(topicName)) {
            activeBroker.channel.queueBind(newQueueName, topicName, "") // bind new queue to an exchange
        } else {
            throw ResourceNotFound("Topic $topicName was not found.")
        }

        val subscription = Subscription(-1, newQueueName, topicName, userRepository.findByLogin(user.login)!!)

        subscriptionRepository.save(subscription)
    }

    override fun unsubscribeTopic(topicName: String, deleteIfLast: Boolean) {
        val user = getAuthenticatedUser()

        val topicSubscriptions = subscriptionRepository.findByExchange(topicName)

        val usersTopic = topicSubscriptions.firstOrNull {
            it.exchange == topicName &&
                    it.user.id == user.id
        }

        if (usersTopic != null) {
            activeBroker.channel.queueDelete(usersTopic.queue)
            subscriptionRepository.delete(usersTopic)
        }

        // if user was last subscriber, delete exchange
        if (topicSubscriptions.count() == 1) {
            activeBroker.channel.exchangeDelete(topicName)
        }
    }

    override fun getTopics(): List<Topic> {
        val exchanges = activeBroker.admin.getExchanges()
        val topics = mutableListOf<Topic>()

        exchanges.filter {
            it.type == ExchangeTypes.FANOUT &&
                    it.name != DEFAULT_TOPIC_EXCHANGE &&
                    it.name != DEFAULT_AMQP_EXCHANGE
        }.forEach {
            val consumersNumber = subscriptionRepository.findByExchange(it.name).count()

            topics.add(Topic(it.name, consumersNumber))
        }

        return topics
    }

    override fun getQueues(): List<Queue> {
        val adminQueues = activeBroker.admin.getQueues()
        val exchanges = activeBroker.admin.getExchanges()

        val resultQueues = mutableListOf<Queue>()

        adminQueues.forEach { adminQueue ->
            val exchangeForQueue = exchanges.firstOrNull { it.name == adminQueue.name && it.type == ExchangeTypes.FANOUT }

            if (exchangeForQueue == null && adminQueue.name != this.messageAuditQueue) {
                val consumersNumber = subscriptionRepository.findByExchangeAndQueue(adminQueue.name, "").count()
                resultQueues.add(Queue(adminQueue.name, consumersNumber))
            }
        }

        return resultQueues
    }

    override fun createQueue(queueName: String) {
        val arguments = mutableMapOf<String, Any>()

        arguments[QUEUE_TTL_ARGUMENT] = getRetentionValueInMilliseconds(retentionHours)

        activeBroker.channel.queueDeclare(queueName, true, false, false, arguments)
    }

    private fun getRetentionValueInMilliseconds(retentionHours: Int): Int {
        return retentionHours * HOURS_TO_MILLISECONDS
    }

    override fun subscribeQueue(queueName: String) {
        val user = getAuthenticatedUser()

        if (subscriptionRepository.findByExchangeAndQueueAndUser("", queueName, user) != null) return

        if (!checkIfQueueExists(queueName)) throw ResourceNotFound("Queue $queueName was not found.")

        val subscription = Subscription(-1, queueName, "", userRepository.findByLogin(user.login)!!)

        subscriptionRepository.save(subscription)
    }

    override fun unsubscribeQueue(queueName: String) {
        val user = getAuthenticatedUser()

        val queueSubscription = subscriptionRepository.findByExchangeAndQueueAndUser("", queueName, user)

        if (queueSubscription != null) {
            subscriptionRepository.delete(queueSubscription)
        }
    }

    override fun unsubscribeAllQueues(user: AuthUserDetails) {
        val userQueues = subscriptionRepository.findByExchangeAndUser("", user)

        userQueues.forEach { subscriptionRepository.delete(it) }
    }

    override fun readFromTopic(topicName: String): Message? {
        val subscription = subscriptionRepository.findByExchangeAndUser(topicName, getAuthenticatedUser()).firstOrNull()
                ?: throw ResourceNotFound("Topic $topicName doesn't exist or you are not a subscriber")

        try {
            var messageBody = String(activeBroker.channel.basicGet(subscription.queue, true).body, StandardCharsets.UTF_8)
            logMessageAudit(MessageAudit(-1, getAuthenticatedUser().login, Date(), MessageDestinationType.TOPIC, topicName,
                    messageBody.length, MessageAudit.DOWNLOAD, activeBroker.broker.id))

            return Message(messageBody)
        } catch (ex: Exception) {
            throw ResourceNotFound("No more messages available in topic $topicName")
        }
    }

    override fun readFromQueue(queueName: String): Message? {
        if (!checkIfQueueExists(queueName)) {
            throw ResourceNotFound("Queue $queueName does not exist.")
        }

        try {
            val messageBody = String(activeBroker.channel.basicGet(queueName, true).body, StandardCharsets.UTF_8)

            logMessageAudit(MessageAudit(-1, getAuthenticatedUser().login, Date(), MessageDestinationType.QUEUE,
                    queueName, messageBody.length, MessageAudit.DOWNLOAD, activeBroker.broker.id))

            return Message(messageBody)
        } catch (ex: Exception) {
            throw ResourceNotFound("No more messages available in queue $queueName")
        }
    }

    override fun publishQueue(message: String, queueName: String) {
        if (checkIfQueueExists(queueName)) {
            activeBroker.channel.basicPublish("", queueName, null, message.toByteArray())
            logMessageAudit(MessageAudit(-1, getAuthenticatedUser().login, Date(), MessageDestinationType.QUEUE, queueName,
                    message.length, MessageAudit.UPLOAD, activeBroker.broker.id))
        } else {
            throw ResourceNotFound("Queue $queueName does not exist.")
        }
    }

    private fun checkIfQueueExists(queueName: String): Boolean {
        return activeBroker.admin.getQueue(queueName) != null
    }

    private fun checkIfExchangeExists(exchangeName: String): Boolean {
        return activeBroker.admin.getExchange(exchangeName) != null
    }

    private fun getAuthenticatedUser(): AuthUserDetails {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.principal as AuthUserDetails
    }

    override fun deleteTopic(topicName: String, forceDelete: Boolean) {
        val user = getAuthenticatedUser()
        val subscriptions = subscriptionRepository.findByExchange(topicName)

        // if user is the only subscriber
        if (subscriptions.count() == 1 && subscriptions.firstOrNull()?.user == user) {
            activeBroker.channel.queueDelete(subscriptions.firstOrNull()?.queue)
            activeBroker.channel.exchangeDelete(topicName)

            subscriptionRepository.delete(subscriptions.first())

        } else if (subscriptions.count() > 1) { // delete all topic queues if forceDelete is true
            if (!forceDelete) {
                throw BadRequest("This topic has more than one subscriber. If you want to delete it anyway, set forceDelete to true.")
            }

            subscriptions.forEach {
                activeBroker.channel.queueDelete(it.queue)
                subscriptionRepository.delete(it)
            }

            activeBroker.channel.exchangeDelete(topicName)
        }
    }

    override fun purgeTopic(topicName: String) {
        val subscriptions = subscriptionRepository.findByExchange(topicName)

        if (subscriptions.count() > 0) {
            subscriptions.forEach {
                activeBroker.channel.queuePurge(it.queue)
            }
        }
    }

    override fun deleteQueue(queueName: String, forceDelete: Boolean) {
        val result = activeBroker.channel.queueDelete(queueName, false, !forceDelete)

        if (result.messageCount != 0 && !forceDelete) {
            throw BadRequest("This queue contains messages. If you want to delete it anyway, set forceDelete to true.")
        }
    }

    override fun purgeQueue(queueName: String) {
        activeBroker.channel.queuePurge(queueName)
    }

    // Logging
    override fun registerMessageLogQueue(queueName: String) {
        this.messageAuditQueue = queueName
        activeBroker.channel.queueDeclare(queueName, true, false, false, null)
    }

    override fun registerMessageAuditLogListener(queueName: String, messageAuditLogger: MessageAuditLogger) {
        this.messageAuditListener = RabbitMessageAuditListener(this.messageAuditQueue, this.activeBroker, this)
        this.messageAuditListener.listenForAuditEvents(messageAuditLogger)
    }

    override fun messageAuditConnectionClosed() {
        activeBroker = getAvailableBrokerConnection()

        this.messageAuditListener.reconnectToBroker(activeBroker)
    }

    override fun logMessageAudit(msgAudit: MessageAudit) {
        val audit = Gson().toJson(msgAudit)

        activeBroker.channel.basicPublish("", this.messageAuditQueue, null, audit.toByteArray())
    }

    override fun setNextAvailableBroker() {
        activeBroker = getAvailableBrokerConnection()
    }

    override fun reviveDeadBrokers() {
        val deadBrokers = brokerRepository.findByStatus(Broker.BROKER_READY)

        deadBrokers.forEach{broker ->
            createBrokerConnection(broker)
        }
    }

    private fun initBrokerConnections() {
        val allBrokers = brokerRepository.findAll()

        allBrokers.forEach { broker ->
            if (broker.status == Broker.BROKER_ONLINE || broker.status == Broker.BROKER_READY) {
                createBrokerConnection(broker)
            } else {
                log.error("Broker ${broker.name} on address ${broker.ip} port: ${broker.port} is offline.")
            }
        }
    }

    override fun setupPolicies(retentionHours: Int, replicationLevel: Int) {
        this.retentionHours = retentionHours
        this.replicationLevel = replicationLevel

    }

    private fun createBrokerConnection(broker: Broker) {
        try {
            brokerConnectors.add(RabbitBrokerConnection(broker, encryptionProperties))
            broker.status = Broker.BROKER_ONLINE

            brokerRepository.save(broker)

            log.info("Broker ${broker.name} on address ${broker.ip} on port: ${broker.port} was SUCCESSFULLY connected.")
        } catch (ex: Exception) {
            broker.status = Broker.BROKER_OFFLINE
            brokerRepository.save(broker)

            log.error("Broker ${broker.name} on address ${broker.ip} on port: ${broker.port} isn't available.", ex)
        }
    }

    override fun getActiveBrokersNumber(): Int {
        return brokerConnectors.size
    }

    companion object {
        private const val FANOUT_EXCHANGE = "fanout"
        private const val DEFAULT_TOPIC_EXCHANGE = "amq.fanout"
        private const val DEFAULT_AMQP_EXCHANGE = ""

        private const val QUEUE_TTL_ARGUMENT = "x-message-ttl"
        private const val HOURS_TO_MILLISECONDS = 3600000

        //Policies
        private const val REPLICATION_POLICY_NAME = "replication-basic"

        private var log = LogManager.getLogger(MessagingService::class.java.name)
    }
}
