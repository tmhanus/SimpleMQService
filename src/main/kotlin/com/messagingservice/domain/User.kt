package com.messagingservice.domain

import javax.persistence.*

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 19-Feb-18.
 */

@Entity
@Table(name = "users")
open class User (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,

    val login: String,

    @Column(name = "password")
    var psswrd: String,

    var active: Int,

    @ManyToMany(cascade = [(CascadeType.ALL)], fetch = FetchType.EAGER)
    @JoinTable(name = "user_role",
            joinColumns = [(JoinColumn(name = "user_id", referencedColumnName = "id"))],
            inverseJoinColumns = [(JoinColumn(name = "role_id", referencedColumnName = "id"))])
    var roles: List<Role> = mutableListOf()
) {
    override fun toString(): String{
        return "{login: ${this.id}, roles: ${roles.map { it->it.role }}}"
    }
}