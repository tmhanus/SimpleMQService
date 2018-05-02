package com.messagingservice.domain

import java.util.*

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 15-Feb-18.
 */

data class Statistics (
    val download: Int,

    val upload: Int,

    val user: String,

    val from: Date,

    val to: Date
)