package com.messagingservice.utils

import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 1-May-18.
 */

class MqEncryptor {
    companion object {

        fun decrypt(encrypted: String, iv: String, key: String): String{
            val ivSpec = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))
            val sKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, sKeySpec, ivSpec)

            return String(cipher.doFinal(Base64.getDecoder().decode(encrypted)))
        }
    }
}