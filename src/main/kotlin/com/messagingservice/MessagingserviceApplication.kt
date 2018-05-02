package com.messagingservice

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication(
        scanBasePackages = ["com.messagingservice.service",
            "com.messagingservice.repository",
            "com.messagingservice.mq",
            "com.messagingservice"])
class MessagingserviceApplication

fun main(args: Array<String>) {

    SpringApplication.run(MessagingserviceApplication::class.java, *args)
}
