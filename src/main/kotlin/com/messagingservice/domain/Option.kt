package com.messagingservice.domain

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 15-Apr-18.
 */

@Entity
@Table(name = "options")
data class Option(
    @Id
    val name: String,

    var value: String
)