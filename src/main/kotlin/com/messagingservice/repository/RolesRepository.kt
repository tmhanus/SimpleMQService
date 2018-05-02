package com.messagingservice.repository

import com.messagingservice.domain.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RolesRepository: JpaRepository<Role, Long>