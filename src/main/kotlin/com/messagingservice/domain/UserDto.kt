package com.messagingservice.domain

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 15-Feb-18.
 */

class UserDto (
    val id: Long,

    val login: String,

    val active: Int,

    var roles: List<Role> = mutableListOf<Role>()
)