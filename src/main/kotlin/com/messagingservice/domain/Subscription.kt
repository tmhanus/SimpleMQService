package com.messagingservice.domain

import javax.persistence.*

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 15-Feb-18.
 */

@Entity
@Table(name = "subscriptions")
data class Subscription (
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
     var id: Long,

     var queue: String,

     var exchange: String,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User
)