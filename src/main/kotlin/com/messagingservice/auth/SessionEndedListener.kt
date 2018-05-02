package com.messagingservice.auth

import com.messagingservice.domain.AuthUserDetails
import com.messagingservice.mq.IMqProvider
import org.springframework.context.ApplicationListener
import org.springframework.security.core.session.SessionDestroyedEvent
import org.springframework.stereotype.Component
/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 05-Mar-18.
 */

@Component
class SessionEndedListener(val mqProvider: IMqProvider) : ApplicationListener<SessionDestroyedEvent> {

    override fun onApplicationEvent(event: SessionDestroyedEvent) {
        for (securityContext in event.securityContexts) {
            val authentication = securityContext.authentication
            val user = authentication.principal as AuthUserDetails

            mqProvider.unsubscribeAllQueues(user)
        }
    }
}