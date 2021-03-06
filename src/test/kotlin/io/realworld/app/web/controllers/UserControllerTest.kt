package io.realworld.app.web.controllers

import io.javalin.util.HttpUtil
import io.kraftverk.core.Kraftverk
import io.kraftverk.core.managed.Managed
import io.realworld.app.AppModule0
import io.realworld.app.domain.User
import io.realworld.app.domain.UserDTO
import io.realworld.app.web.server.ErrorResponse
import org.eclipse.jetty.http.HttpStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserControllerTest {
    private lateinit var app: Managed<AppModule0>
    private lateinit var http: HttpUtil

    @BeforeEach
    fun start() {
        app = Kraftverk.start { AppModule0() }
        http = HttpUtil(app { port })
    }

    @AfterEach
    fun stop() {
        app.stop()
    }

    @Test
    fun `invalid login without pass valid body`() {
        val response = http.post<ErrorResponse>("/api/users/login",
                UserDTO())

        assertEquals(response.status, HttpStatus.UNPROCESSABLE_ENTITY_422)
        assertTrue(response.body.errors["body"]!!.contains("can't be empty or invalid"))
    }

    @Test
    fun `success login with email and password`() {
        val email = "success_login@valid_email.com"
        val password = "Test"
        http.registerUser(email, password, "user_name_test")
        val userDTO = UserDTO(User(email = email, password = password))
        val response = http.post<UserDTO>("/api/users/login", userDTO)

        assertEquals(response.status, HttpStatus.OK_200)
        assertEquals(response.body.user?.email, userDTO.user?.email)
        assertNotNull(response.body.user?.token)
    }

    @Test
    fun `success register user`() {
        val userDTO = UserDTO(User(email = "success_register@valid_email.com", password = "Test", username =
        "username_test"))
        val response = http.post<UserDTO>("/api/users", userDTO)

        assertEquals(response.status, HttpStatus.OK_200)
        assertEquals(response.body.user?.username, userDTO.user?.username)
        assertEquals(response.body.user?.password, userDTO.user?.password)
    }

    @Test
    fun `invalid get current user without token`() {
        val response = http.get<ErrorResponse>("/api/user")

        assertEquals(response.status, HttpStatus.FORBIDDEN_403)
    }

    @Test
    fun `get current user by token`() {
        val email = "get_current@valid_email.com"
        val password = "Test"
        http.registerUser(email, password, "username_Test")
        http.loginAndSetTokenHeader(email, password)
        val response = http.get<UserDTO>("/api/user")

        assertEquals(response.status, HttpStatus.OK_200)
        assertNotNull(response.body.user?.username)
        assertNotNull(response.body.user?.password)
        assertNotNull(response.body.user?.token)
    }

    @Test
    fun `update user data`() {
        val email = "email_valid@valid_email.com"
        val password = "Test"
        http.registerUser(email, password, "username_Test")

        http.loginAndSetTokenHeader("email_valid@valid_email.com", "Test")
        val userDTO = UserDTO(User(email = "update_user@update_test.com", password = "Test"))
        val response = http.put<UserDTO>("/api/user", userDTO)

        assertEquals(response.status, HttpStatus.OK_200)
        assertEquals(response.body.user?.email, userDTO.user?.email)
    }
}