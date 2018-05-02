package com.messagingservice.domain

import javax.persistence.*

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 21-Feb-18.
 */

@Entity
@Table(name = "brokers")
data class Broker(
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long,

    var name: String,

    var ip: String,

    var port: Int,

    @Column(name = "admin_port")
    var adminPort: Int,

    @Column(name = "admin_login")
    var adminLogin: String,

    @Column(name = "admin_password")
    var adminPassword: String,

    var status: String
) {
    companion object {
        const val BROKER_ONLINE = "ONLINE"
        const val BROKER_OFFLINE = "OFFLINE"
        const val BROKER_READY = "READY"
    }
}
