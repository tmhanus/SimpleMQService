package com.messagingservice.log

import com.messagingservice.domain.MessageAudit
import com.messagingservice.repository.MessageAuditRepository
import org.springframework.stereotype.Component

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 15-Mar-18.
 */

@Component
class MessageAuditLogger(private val messageAuditRepository: MessageAuditRepository){
    var messageAuditBatchSize: Int = 1

    private var logsBatchCount = 0
    private val auditLogs = mutableListOf<MessageAudit>()

    fun logMessageAudit(messageAudit: MessageAudit) {
        logsBatchCount++
        auditLogs.add(messageAudit)

        if (logsBatchCount == messageAuditBatchSize) {

            logBatchToDb()

            emptyLogsBatch()
        }
    }

    private fun logBatchToDb() {
        messageAuditRepository.saveAll(auditLogs)
    }

    private fun emptyLogsBatch() {
        logsBatchCount = 0
        auditLogs.clear()
    }
}