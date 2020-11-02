package io.realworld.app.web.controllers

import io.javalin.util.HttpUtil
import io.kraftverk.core.Kraftverk
import io.kraftverk.core.managed.Managed
import io.realworld.app.AppModule0
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.TagDTO
import org.eclipse.jetty.http.HttpStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TagControllerTest {
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
    fun `get all tags`() {
        val article = Article(title = "How to train your dragon",
                description = "Ever wonder how?",
                body = "Very carefully.",
                tagList = listOf("dragons", "training"))
        val email = "create_article@valid_email.com"
        val password = "Test"
        http.registerUser(email, password, "user_name_test")
        http.loginAndSetTokenHeader(email, password)

        http.post<ArticleDTO>("/api/articles", ArticleDTO(article))

        val response = http.get<TagDTO>("/api/tags")

        assertEquals(response.status, HttpStatus.OK_200)
        assertTrue(response.body.tags.isNotEmpty())
    }
}