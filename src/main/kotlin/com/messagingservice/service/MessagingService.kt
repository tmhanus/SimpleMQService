package com.messagingservice.service

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.messagingservice.domain.*
import com.messagingservice.log.MessageAuditLogger
import com.messagingservice.mq.IMqProvider
import com.messagingservice.repository.MessageAuditRepository
import com.messagingservice.repository.OptionsRepository
import com.messagingservice.repository.RolesRepository
import com.messagingservice.repository.UserRepository
import org.apache.logging.log4j.LogManager
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 21-Feb-18.
 */

interface IMessagingService {
    fun createTopic(body: String)

    fun listTopics(): List<Topic>

    fun listQueues(): String

    fun publishTopic(topicName: String, body: String)

    fun subscribeTopic(topicName: String)//: String

    fun unsubscribeTopic(topicName: String, deleteIfLast: Boolean)

    fun readFromTopic(topicName: String): String

    fun createQueue(body: String)

    fun deleteQueue(queueName: String, forceDelete: Boolean)

    fun purgeQueue(queueName: String)

    fun publishQueue(queueName: String, body: String)

    fun readFromQueue(queueName: String): String

    fun deleteTopic(topicName: String, forceDelete: Boolean)

    fun purgeTopic(topicName: String)

    fun getStatistics(userName: String?, from: Date?, to: Date?, destinationType: String?) : Statistics
}

@RestController
@RequestMapping("/api")
class MessagingService(private val mqProvider: IMqProvider,
                       val optionsRepository: OptionsRepository,
                       var messageAuditLogger: MessageAuditLogger,
                       var messageAuditRepository: MessageAuditRepository,
                       var userRepository: UserRepository,
                       var rolesRepository: RolesRepository,
                       var passwordEncoder: PasswordEncoder)
    : IMessagingService {

    // DEFAULT SERVICE SETTINGS
    private var retentionHours: Int = UNLIMITED_RETENTION
    private var replicationLevel: Int = NO_REPLICATION

    // UPLOAD
    private var uploadDailyLimitBytes: Int = -1
    private var uploadedTodayCounter: Int = 0
    private var lastUploadedDataCounterTimestamp = Date(0)

    // DOWNLOAD
    private var downloadDailyLimitBytes: Int = -1
    private var downloadedTodayCounter: Int = 0
    private var lastDownloadedDataCounterTimestamp = Date(0)

    private var dataTransferredRefreshInterval: Int = DEFAULT_DATA_TRANSFERRED_REFRESH_INTERVAL

    // BROKERS
    private var lastDeadBrokersRecoveryTimestamp = Date(0)
    private var lastBrokersNumberCheckTimestamp = Date(0)
    private var deadBrokersRecoveryInterval: Int = DEFAULT_DEAD_BROKERS_RECOVERY_INTERVAL
    private var sufficientBrokersNumberTestInterval: Int = DEFAULT_SUFFICIENT_BROKERS_NUMBER_TEST_INTERVAL

    // AUDIT
    private var messageAuditBatchSize: Int = DEFAULT_MESSAGE_AUDIT_BATCH_SIZE

    init {
        setupMqPolicies()

        messageAuditLogger.messageAuditBatchSize = messageAuditBatchSize

        mqProvider.run {
            registerMessageLogQueue(MESSAGE_AUDIT_QUEUE)
            registerMessageAuditLogListener(MESSAGE_AUDIT_QUEUE, messageAuditLogger)
        }
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @GetMapping("/topics")
    override fun listTopics(): List<Topic> {
        setupBrokersForRequest()
        return mqProvider.getTopics()
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @GetMapping("/queues")
    override fun listQueues(): String {
        setupBrokersForRequest()
        return mqProvider.getQueues().toString()
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @PostMapping("/topics/{topicName}/publish")
    override fun publishTopic(@PathVariable topicName: String, @RequestBody body: String) {
        if (body.isEmpty()) {
            throw IllegalArgumentException("Body is missing in the request.")
        }
        val jsonObject = Gson().fromJson(body, JsonObject::class.java)

        val messageContent = jsonObject?.get("message")?.asString
                ?: throw IllegalArgumentException("Message is a required body argument.")

        if (!checkUploadLimit(messageContent)) {
            throw LimitExceeded("You exceeded your daily upload limit. If you want to proceed, please contact your administrator")
        }

        setupBrokersForRequest()

        mqProvider.publishTopic(messageContent, topicName)
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @PostMapping("/topics/{topicName}/subscribe")
    override fun subscribeTopic(@PathVariable topicName: String) {//: String {
        setupBrokersForRequest()
        mqProvider.subscribeTopic(topicName)
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @PostMapping("/topics/{topicName}/unsubscribe")
    override fun unsubscribeTopic(@PathVariable topicName: String, @RequestParam(value = "deleteIfLast", defaultValue = "0") deleteIfLast: Boolean) {
        setupBrokersForRequest()
        mqProvider.unsubscribeTopic(topicName, deleteIfLast)
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @DeleteMapping("/topics/{topicName}")
    override fun deleteTopic(@PathVariable topicName: String, @RequestParam(value = "forceDelete", defaultValue = "0") forceDelete: Boolean) {
        setupBrokersForRequest()
        mqProvider.deleteTopic(topicName, forceDelete)
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @PostMapping("/topics/{topicName}/purge")
    override fun purgeTopic(@PathVariable topicName: String) {
        setupBrokersForRequest()
        mqProvider.purgeTopic(topicName)
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @GetMapping("/topics/{topicName}/receive")
    override fun readFromTopic(@PathVariable topicName: String): String {
        if (!checkDownloadLimit()) {
            throw LimitExceeded("You exceeded your daily download limit. If you want to proceed, please contact your administrator")
        }

        setupBrokersForRequest()

        val messageContent = mqProvider.readFromTopic(topicName)

        val jsonResult = Gson().toJson(messageContent)

        // if last message exceeded limit, update counter and don't wait for the downloaded counter cache to expire
        updateDownloadedCounterIfLimitExceeded(messageContent?.payload)

        return jsonResult
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @PutMapping("/topics")
    override fun createTopic(@RequestBody body: String) {
        if (body.isEmpty()) {
            throw IllegalArgumentException("Body is missing in the request.")
        }

        val name = getRequiredFieldValue("name", Gson().fromJson(body, JsonObject::class.java))

        setupBrokersForRequest()
        mqProvider.createTopic(name)

        log.info("Topic $name was Successfully created.")
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @PutMapping("/queues")
    override fun createQueue(@RequestBody body: String) {
        if (body.isEmpty()) {
            throw IllegalArgumentException("Body is missing in the request.")
        }

        val name = getRequiredFieldValue("name", Gson().fromJson(body, JsonObject::class.java))

        if (name == MESSAGE_AUDIT_QUEUE) {
            throw BadRequest("Queue name $name is reserved name and you can't use it.")
        }

        setupBrokersForRequest()
        try {
            mqProvider.createQueue(name)
            log.info("Queue $name was Successfully created.")
        } catch (ex: NumberFormatException) {
            log.error("Could not create queue $name", ex)
            throw InternalError("Could not create queue $name due to some internal error.")
        }
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @DeleteMapping("/queues/{queueName}")
    override fun deleteQueue(@PathVariable queueName: String, @RequestParam(value = "forceDelete", defaultValue = "0") forceDelete: Boolean) {

        if (queueName == MESSAGE_AUDIT_QUEUE) {
            throw BadRequest("Queue name $queueName is reserved name and you can't delete it.")
        }

        setupBrokersForRequest()
        mqProvider.deleteQueue(queueName, forceDelete)
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @PostMapping("/queues/{queueName}/purge")
    override fun purgeQueue(@PathVariable queueName: String) {
        if (queueName == MESSAGE_AUDIT_QUEUE) {
            throw BadRequest("Queue name $queueName is reserved name and you can't purge it.")
        }

        setupBrokersForRequest()
        mqProvider.purgeQueue(queueName)
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @PostMapping("/queues/{queueName}/publish")
    override fun publishQueue(@PathVariable queueName: String, @RequestBody body: String) {
        if (queueName == MESSAGE_AUDIT_QUEUE) {
            throw BadRequest("Queue name $queueName is a system queue and you can't publish to it.")
        }

        if (body.isEmpty()) {
            throw IllegalArgumentException("Body is missing in the request.")
        }

        val messageContent = getRequiredFieldValue("message", Gson().fromJson(body, JsonObject::class.java))

        if (!checkUploadLimit(messageContent)) {
            throw LimitExceeded("You exceeded your daily upload limit. If you want to proceed, please contact your administrator")
        }

        setupBrokersForRequest()

        mqProvider.publishQueue(messageContent, queueName)
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @GetMapping("/queues/{queueName}/receive")
    override fun readFromQueue(@PathVariable queueName: String): String {
        if (queueName == MESSAGE_AUDIT_QUEUE) {
            throw BadRequest("Queue name $queueName is a system queue and you can't read from it.")
        }

        if (!checkDownloadLimit()) {
            throw LimitExceeded("You exceeded your daily download limit. If you want to proceed, please contact your administrator")
        }

        setupBrokersForRequest()

        val messageContent = mqProvider.readFromQueue(queueName)

        val jsonResult = Gson().toJson(messageContent)

        // if last message exceeded limit, update counter and don't wait for the downloaded counter cache to expire
        updateDownloadedCounterIfLimitExceeded(messageContent?.payload)

        return jsonResult
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    @PostMapping("/stats")
    override fun getStatistics(@RequestParam("user", required = false) userName: String?,
                               @RequestParam("from", required = false) @DateTimeFormat(pattern="yyyy-MM-dd hh:mm:ss") from: Date?,
                               @RequestParam("to", required = false) @DateTimeFormat(pattern="yyyy-MM-dd hh:mm:ss") to: Date?,
                               @RequestParam("destinationType", required = false) destinationType: String?): Statistics{

        val requestedFromDate = from ?: getMidnightTime(0)
        val requestedToDate = to ?: getMidnightTime(1)

        var messageAudits = messageAuditRepository
                .findAllByTimestampGreaterThanEqualAndTimestampLessThanEqual(requestedFromDate, requestedToDate)

        if (userName != null){
            messageAudits = messageAudits
                    .filter { it.user == userName }
                    .map { MessageAudit(it.id, it.user, it.timestamp, it.destType, it.destName, it.size, it.traffic, it.broker) }
        }

        if (destinationType != null) {
            messageAudits = messageAudits
                    .filter { it.destType == MessageDestinationType.valueOf(destinationType)}
                    .map { MessageAudit(it.id, it.user, it.timestamp, it.destType, it.destName, it.size, it.traffic, it.broker) }
        }

        val uploaded = messageAudits
                .filter { it.traffic == MessageAudit.UPLOAD }
                .map { it.size }.sum()

        val downloaded = messageAudits
                .filter { it.traffic == MessageAudit.DOWNLOAD }
                .map { it.size }.sum()

        return Statistics(downloaded, uploaded, userName ?: "ALL", requestedFromDate, requestedToDate)
    }

    @PreAuthorize("hasAnyRole('admin')")
    @PutMapping("/users")
    fun createUser(@RequestBody body: String) {
        val jsonObject = Gson().fromJson(body, JsonObject::class.java)

        val userName = getRequiredFieldValue("username", jsonObject)
        val password = getRequiredFieldValue("password", jsonObject)
        val role = getRequiredFieldValue("role", jsonObject)

        val existingRoles = rolesRepository.findAll()

        val userRoles = mutableListOf<Role>()

        when (role) {
            Role.ADMIN_ROLE -> userRoles.addAll(existingRoles)
            Role.USER_ROLE -> userRoles.add(existingRoles.first { it.role == Role.USER_ROLE})
            else -> throw ResourceNotFound("Specified role does not exist.")
        }

        val encodedPassword = passwordEncoder.encode(password)

        userRepository.save(User(-1, userName, encodedPassword, 1, userRoles ))
    }

    @PreAuthorize("hasAnyRole('admin')")
    @GetMapping("/users")
    fun listUsers(): List<UserDto> {
        val users = mutableListOf<UserDto>()

        userRepository.findAll().forEach { users.add(UserDto(it.id, it.login, it.active, it.roles)) }

        return users
    }

    @PreAuthorize("hasAnyRole('admin')")
    @PostMapping("/users/{username}")
    fun updateUser(@PathVariable("username") userName: String,
                   @RequestBody body: String) {

        val jsonObject = Gson().fromJson(body, JsonObject::class.java)

        val password = getOptionalFieldValue("password", jsonObject)
        val role = getRequiredFieldValue("role", jsonObject)
        val active = getRequiredFieldValue("active", jsonObject).toIntOrNull() ?:
                throw BadRequest("Field active is supposed be 0 or 1.")

        val user = userRepository.findByLogin(userName)
                ?: throw ResourceNotFound("Specified user does not exist.")

        val allExistingRoles = rolesRepository.findAll()

        if (password != null) {
            val hashedPassword = passwordEncoder.encode(password)
            user.psswrd = hashedPassword
        }

        user.active = active

        val userRoles = mutableListOf<Role>()

        when (role) {
            Role.ADMIN_ROLE -> userRoles.addAll(allExistingRoles)
            Role.USER_ROLE -> userRoles.add(allExistingRoles.first { it.role == Role.USER_ROLE})
            else -> throw ResourceNotFound("Specified role does not exist.")
        }

        userRepository.save(user)
    }

    @PreAuthorize("hasAnyRole('admin')")
    @DeleteMapping("/users/{username}")
    fun deleteUser(@PathVariable("username") userName: String) {
        if (getAuthenticatedUser().login == userName) {
            throw BadRequest("You can't delete yourself.")
        }

        val user = userRepository.findByLogin(userName)
                ?: throw ResourceNotFound("Specified user does not exist.")

        if (messageAuditRepository.findByUser(user.login).any()){
            user.active = 0
            userRepository.save(user)
        } else {
            userRepository.delete(user)
        }
    }

    private fun updateDownloadedCounterIfLimitExceeded(payload: String?) {
        val length = payload?.length ?: 0

        if (downloadedTodayCounter + length >= downloadDailyLimitBytes) {
            downloadedTodayCounter += length
        }
    }

    private fun setupMqPolicies() {
        val options: HashMap<String, String> = hashMapOf()

        optionsRepository.findAll().forEach { options[it.name] = it.value }

        retentionHours = options[RETENTION_HOURS]?.toIntOrNull() ?: UNLIMITED_RETENTION
        replicationLevel = options[REPLICATION_LEVEL]?.toIntOrNull() ?: NO_REPLICATION
        uploadDailyLimitBytes = options[UPLOAD_DAILY_LIMIT]?.toIntOrNull() ?: UNLIMITED_TRANSFER
        downloadDailyLimitBytes = options[DOWNLOAD_DAILY_LIMIT]?.toIntOrNull() ?: UNLIMITED_TRANSFER
        messageAuditBatchSize = options[MESSAGE_AUDIT_BATCH_SIZE]?.toIntOrNull() ?: DEFAULT_MESSAGE_AUDIT_BATCH_SIZE
        deadBrokersRecoveryInterval = options[DEAD_BROKERS_RECOVERY_INTERVAL]?.toIntOrNull() ?: DEFAULT_DEAD_BROKERS_RECOVERY_INTERVAL
        sufficientBrokersNumberTestInterval = options[SUFFICIENT_BROKERS_NUMBER_TEST_INTERVAL]?.toIntOrNull() ?: DEFAULT_SUFFICIENT_BROKERS_NUMBER_TEST_INTERVAL
        dataTransferredRefreshInterval = options[DATA_TRANSFERRED_REFRESH_INTERVAL]?.toIntOrNull() ?: DEFAULT_DATA_TRANSFERRED_REFRESH_INTERVAL

        uploadDailyLimitBytes = if (uploadDailyLimitBytes == -1) -1 else uploadDailyLimitBytes * MB_TO_BYTE
        downloadDailyLimitBytes = if (downloadDailyLimitBytes == -1) -1 else downloadDailyLimitBytes * MB_TO_BYTE

        mqProvider.setupPolicies(retentionHours, replicationLevel)
    }


    private fun checkUploadLimit(msgBody: String): Boolean {
        val timePeriod = Date().time - lastUploadedDataCounterTimestamp.time

        val yesterdayMidnight = getMidnightTime(0)
        val todayMidnight = getMidnightTime(1)

        if (timePeriod >= dataTransferredRefreshInterval) {
            uploadedTodayCounter = messageAuditRepository.getTransferredDataBetween(yesterdayMidnight, todayMidnight, "UPLOAD") ?: 0

            lastUploadedDataCounterTimestamp = Date() // Set last upload counter update to current time
        }

        if (uploadDailyLimitBytes == UNLIMITED_TRANSFER) {
            return true
        }

        if (uploadedTodayCounter + msgBody.length >= uploadDailyLimitBytes) {
            uploadedTodayCounter += msgBody.length
            return false
        }

        return true
    }

    private fun getMidnightTime(offset: Int): Date {
        val cal = GregorianCalendar()
        cal.set(Calendar.HOUR_OF_DAY, 0) //anything 0 - 23
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        dateFormat.calendar = cal

        cal.add(Calendar.DATE, offset)

        return dateFormat.parse(dateFormat.format(cal.time)) ?: Date()
    }

    private fun checkDownloadLimit(): Boolean {
        val timePeriod = Date().time - lastDownloadedDataCounterTimestamp.time

        val yesterdayMidnight = getMidnightTime(0)
        val todayMidnight = getMidnightTime(1)

        if (timePeriod >= dataTransferredRefreshInterval) {
            uploadedTodayCounter = messageAuditRepository.getTransferredDataBetween(yesterdayMidnight, todayMidnight, "DOWNLOAD") ?: 0

            lastUploadedDataCounterTimestamp = Date() // Set last upload counter update to current time
        }

        if (downloadDailyLimitBytes == UNLIMITED_TRANSFER) {
            return true
        }

        if (downloadedTodayCounter >= downloadDailyLimitBytes) {
            return false
        }

        return true
    }

    private fun setupBrokersForRequest() {
        reviveDeadBrokers()

        checkBrokersNumber()

        mqProvider.setNextAvailableBroker()
    }

    private fun reviveDeadBrokers() {
        val deadBrokersRecoveryTimePeriod = Date().time - lastDeadBrokersRecoveryTimestamp.time

        if (deadBrokersRecoveryTimePeriod >= deadBrokersRecoveryInterval) {
            mqProvider.reviveDeadBrokers()
            lastDeadBrokersRecoveryTimestamp = Date()
        }
    }

    private fun checkBrokersNumber() {
        val sufficientBrokerNumberCheckTimePeriod = Date().time - lastBrokersNumberCheckTimestamp.time

        if (sufficientBrokerNumberCheckTimePeriod >= sufficientBrokersNumberTestInterval) {
            val numOfBrokers = mqProvider.getActiveBrokersNumber()
            lastBrokersNumberCheckTimestamp = Date()

            if (replicationLevel != NO_REPLICATION && replicationLevel < numOfBrokers) {
                log.warn("Insufficient number of brokers to secure replication level. Available only $numOfBrokers of $replicationLevel broker(s).")
            }
        }
    }

    private fun getAuthenticatedUser(): AuthUserDetails {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.principal as AuthUserDetails
    }

    private fun getRequiredFieldValue(fieldName: String, jsonObject: JsonObject?): String {
        return  jsonObject?.get(fieldName)?.asString
                ?: throw IllegalArgumentException("$fieldName is a required body argument.")
    }

    private fun getOptionalFieldValue(fieldName: String, jsonObject: JsonObject?): String? {
        return  jsonObject?.get(fieldName)?.asString
    }

    companion object {
        const val RETENTION_HOURS = "retention_hours"
        const val REPLICATION_LEVEL = "replication_level"
        const val UPLOAD_DAILY_LIMIT = "data_upload_limit_mb"
        const val DOWNLOAD_DAILY_LIMIT = "data_download_limit_mb"
        const val MESSAGE_AUDIT_BATCH_SIZE = "message_audit_batch_size"
        const val DEAD_BROKERS_RECOVERY_INTERVAL = "dead_brokers_recovery_interval_ms"
        const val SUFFICIENT_BROKERS_NUMBER_TEST_INTERVAL = "sufficient_brokers_number_test_interval_ms"
        const val DATA_TRANSFERRED_REFRESH_INTERVAL = "data_transferred_refresh_interval_ms" // milliseconds

        const val UNLIMITED_TRANSFER = -1
        const val UNLIMITED_RETENTION = 0
        const val NO_REPLICATION = 1
        private const val DEFAULT_MESSAGE_AUDIT_BATCH_SIZE = 3
        private const val DEFAULT_DEAD_BROKERS_RECOVERY_INTERVAL = 60000
        private const val DEFAULT_SUFFICIENT_BROKERS_NUMBER_TEST_INTERVAL = 30000
        private const val DEFAULT_DATA_TRANSFERRED_REFRESH_INTERVAL = 60000

        const val MESSAGE_AUDIT_QUEUE = "internal_message_audit"

        const val MB_TO_BYTE = 1000000

        private var log = LogManager.getLogger(MessagingService::class.java.name)
    }
}