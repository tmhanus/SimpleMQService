package com.messagingservice.repository

import com.messagingservice.domain.Subscription
import com.messagingservice.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 26-Feb-18.
 */

@Repository
interface SubscriptionRepository: JpaRepository<Subscription, Long> {
    fun findByExchange(exchange: String): List<Subscription>

    fun findByExchangeAndQueue(exchange: String, queue: String): List<Subscription>

    fun findByExchangeAndUser(exchange: String, user: User): List<Subscription>

    fun findByExchangeAndQueueAndUser(exchange: String, queue: String, user: User): Subscription?
}