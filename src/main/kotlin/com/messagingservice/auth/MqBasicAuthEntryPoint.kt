package com.messagingservice.auth

import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.io.PrintWriter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 12-Mar-18.
 */

@Component
class MqBasicAuthEntryPoint : BasicAuthenticationEntryPoint() {

    override fun commence(
            request: HttpServletRequest?,
            response: HttpServletResponse?,
            authException: org.springframework.security.core.AuthenticationException? ) {

        //Authentication failed, send error response.
        if (response != null) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.addHeader("WWW-Authenticate", "Basic realm=$realmName")
        }

        var writer : PrintWriter = response!!.writer

        writer.println("HTTP Status 401 : " + authException!!.message)
    }

    override fun afterPropertiesSet() {
        realmName = "MY_TEST_REALM"
        super.afterPropertiesSet()
    }
}