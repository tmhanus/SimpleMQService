package com.messagingservice.domain

import java.util.*
import javax.persistence.*

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 15-Apr-18.
 */

@Entity
@Table(name = "message_audit")
data class MessageAudit (
        @Id @GeneratedValue(strategy = GenerationType.AUTO)
        var id: Long,

        var user: String,

        @Temporal(TemporalType.DATE)
        var timestamp: Date,

        @Column(name = "destination_type")
        var destType: MessageDestinationType,

        @Column(name = "destination_name")
        var destName: String,

        var size: Int,

        var traffic: String,

        @Column(name = "broker")
        var broker: Long
) {
    companion object {
        const val UPLOAD = "UPLOAD"
        const val DOWNLOAD = "DOWNLOAD"
    }
}

enum class MessageDestinationType {
    QUEUE {
        override fun toString(): String {
            return "QUEUE"
        }
    },
    TOPIC {
        override fun toString(): String {
            return "TOPIC"
        }
    }
}