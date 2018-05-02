package com.messagingservice.repository

import com.messagingservice.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 19-Feb-18.
 */

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByLogin(login: String): User?
}