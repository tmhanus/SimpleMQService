package com.messagingservice.utils

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 1-May-18.
 */

@Component
@ConfigurationProperties("encryption")
class EncryptionProperties {
    val aes = Aes()

    class Aes {
        lateinit var key: String
        lateinit var iv: String
    }
}