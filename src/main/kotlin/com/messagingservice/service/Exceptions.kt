package com.messagingservice.service

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 26-Feb-18.
 */

@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFound (override var message:String) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequest (override var message:String) : RuntimeException(message)

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class InternalError (override var message:String) : RuntimeException(message)

@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
class LimitExceeded (override var message: String): RuntimeException(message)