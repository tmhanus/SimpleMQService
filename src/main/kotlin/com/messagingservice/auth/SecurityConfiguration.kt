package com.messagingservice.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder


/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 05-Mar-18.
 */

@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@EnableJpaRepositories(basePackages = ["com.messagingservice.service"])
@Configuration
class SecurityConfiguration(
        var entryPoint: MqBasicAuthEntryPoint
) : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {

        http.csrf().disable()
        http.authorizeRequests()
            .antMatchers("**/api/**").permitAll()
            .anyRequest().authenticated()
        .and()
            .httpBasic()
            .realmName(MQ_REALM)
            .authenticationEntryPoint(entryPoint)
        .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        .and()
            .logout()
            .permitAll()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    companion object {
        private const val MQ_REALM: String = "MQ_REALM"
    }
}