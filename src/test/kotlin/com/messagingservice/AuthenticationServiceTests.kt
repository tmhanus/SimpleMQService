package com.messagingservice

/**
 * Created by Bc. Tomáš Hanus, xhanus11 on 20-Feb-18.
 */

import com.messagingservice.domain.User
import com.messagingservice.repository.UserRepository
import com.messagingservice.service.AuthenticationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.assertj.core.api.Assertions.*
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationServiceTests {
    private lateinit var repository: UserRepository
    private lateinit var service: AuthenticationService

//    private var allUsers: List<User> = listOf(User(1, "JohnMcLane", "password),
//            User(2, "FakeUser", "fakepasswd"))

    @BeforeEach
    fun init() {
        repository = mock(UserRepository::class.java)
        service = AuthenticationService(repository)
    }

    @Test
    fun `basic get first`() {
        // prepare
//        `when`(repository.findAll()).thenReturn(allUsers)

        //execute
//        val firstUser = service.getFirstUser()

        //assert
//        assertThat(firstUser).isEqualTo(allUsers.first())
    }
}