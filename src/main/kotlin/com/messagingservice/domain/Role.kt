package com.messagingservice.domain

import javax.persistence.*

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 15-Feb-18.
 */

@Entity
@Table(name = "roles")
data class Role (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id : Int,

    val role: String = ""
) {
    companion object {
        const val ADMIN_ROLE = "ADMIN"
        const val USER_ROLE = "USER"
    }
}