package com.messagingservice.domain

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 15-Feb-18.
 */

data class Queue(
    val name: String,

    val consumersNumber: Int = 0
)