package com.messagingservice.repository

import com.messagingservice.domain.MessageAudit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 16-Mar-18.
 */

@Repository
interface MessageAuditRepository  : JpaRepository<MessageAudit, Long> {
    @Query(value ="SELECT Sum(size) FROM message_audit WHERE (timestamp BETWEEN ?1 AND ?2) AND traffic = ?3", nativeQuery = true)
    fun getTransferredDataBetween(fromDate: Date, toDate: Date, traffic: String): Int?

    fun findAllByTimestampGreaterThanEqualAndTimestampLessThanEqual(fromDate: Date, toDate: Date): List<MessageAudit>

    fun findByUser(user: String): List<MessageAudit>
}