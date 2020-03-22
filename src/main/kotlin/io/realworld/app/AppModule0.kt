package io.realworld.app

import com.zaxxer.hikari.HikariDataSource
import io.kraftverk.module.*
import io.realworld.app.domain.repository.ArticleRepository
import io.realworld.app.domain.repository.CommentRepository
import io.realworld.app.domain.repository.TagRepository
import io.realworld.app.domain.repository.UserRepository
import io.realworld.app.domain.service.ArticleService
import io.realworld.app.domain.service.CommentService
import io.realworld.app.domain.service.TagService
import io.realworld.app.domain.service.UserService
import io.realworld.app.utils.JwtService
import io.realworld.app.web.AuthService
import io.realworld.app.web.controllers.*
import io.realworld.app.web.route
import io.realworld.app.web.server
import org.h2.tools.Server

class AppModule0 : Module() {

    // Http
    val context by string("http.context")
    val port by port("http.port")

    val authService by bean { AuthService(jwtService()) }

    val server by server(authService, context, port) {
        route(userController())
        route(profileController())
        route(articleController(), commentController())
        route(tagController())
    }

    // User beans
    val userController by bean {
        UserController(userService()) }
    val userService by bean { UserService(jwtService(), userRepository()) }
    val userRepository by bean { UserRepository(dataSource()) }

    // Article beans
    val articleController by bean { ArticleController(articleService()) }
    val articleService by bean { ArticleService(articleRepository(), userRepository()) }
    val articleRepository by bean { ArticleRepository(dataSource()) }

    // Profile beans
    val profileController by bean { ProfileController(userService()) }

    // Comment beans
    val commentController by bean { CommentController(commentService()) }
    val commentService by bean { CommentService(commentRepository()) }
    val commentRepository by bean { CommentRepository(dataSource()) }

    // Tag beans
    val tagController by bean { TagController(tagService()) }
    val tagService by bean { TagService(tagRepository()) }
    val tagRepository by bean { TagRepository(dataSource()) }

    // Jwt
    val jwtService by bean { JwtService() }

    // Jdbc
    val url by string("jdbc.url")
    val username by string("jdbc.username")
    val password by string("jdbc.password", secret = true)
    val dataSource by bean { HikariDataSource() }
    val h2Server by bean { Server.createWebServer() }

    init {
        customize(dataSource) { ds ->
            ds.jdbcUrl = url()
            ds.username = username()
            ds.password = password()
        }
        onCreate(server) { it.start() }
        onDestroy(server) { it.stop() }
        onCreate(h2Server) { it.start() }
        onDestroy(h2Server) { it.stop() }
    }

}
