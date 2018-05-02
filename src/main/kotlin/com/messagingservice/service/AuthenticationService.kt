package com.messagingservice.service

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 19-Feb-18.
 */

import com.messagingservice.domain.AuthUserDetails
import com.messagingservice.domain.User
import com.messagingservice.repository.UserRepository
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@EnableJpaRepositories(basePackages = arrayOf("com.messagingservice.repository" ))
@Service
class AuthenticationService(var userRepository : UserRepository) : UserDetailsService {

    override fun loadUserByUsername(userName: String): UserDetails {
        var user: User? = userRepository.findByLogin(userName)

        if (user === null){
            throw UsernameNotFoundException("Username not found")
        }

        return AuthUserDetails(user.id, user.login, user.psswrd, user.active, user.roles)
    }

}