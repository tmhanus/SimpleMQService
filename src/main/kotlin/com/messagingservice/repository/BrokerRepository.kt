package com.messagingservice.repository

import com.messagingservice.domain.Broker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 26-Feb-18.
 */

@Repository
interface BrokerRepository : JpaRepository<Broker, Long> {
    fun findByStatus(status: String): List<Broker>
}