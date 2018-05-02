package com.messagingservice.domain

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.io.Serializable
import java.util.stream.Collectors

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 06-Mar-18.
 */

class AuthUserDetails ( id: Long, login: String, psswrd: String, active: Int, roles: List<Role> = emptyList())
    : User(id, login, psswrd, active, roles), UserDetails,
    Serializable {

    // Because of serialization
    public val serialVersionUID: Long = 42L

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return roles.stream().map{role -> SimpleGrantedAuthority("ROLE_" + role.role)}
                .collect((Collectors.toList()))
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun getUsername(): String {
        return login
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun getPassword(): String {
        return psswrd
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }
}