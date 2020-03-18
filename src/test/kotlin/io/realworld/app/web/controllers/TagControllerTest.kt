package io.realworld.app.web.controllers

import io.javalin.util.HttpUtil
import io.kraftverk.Kraftverk
import io.kraftverk.managed.Managed
import io.kraftverk.managed.invoke
import io.kraftverk.managed.stop
import io.realworld.app.AppModule0
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.TagDTO
import org.eclipse.jetty.http.HttpStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TagControllerTest {
    private lateinit var app: Managed<AppModule0>
    private lateinit var http: HttpUtil

    @Before
    fun start() {
        app = Kraftverk.start { AppModule0() }
        http = HttpUtil(app { port })
    }

    @After
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