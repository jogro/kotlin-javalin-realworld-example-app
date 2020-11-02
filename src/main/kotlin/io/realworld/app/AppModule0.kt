package io.realworld.app

import com.zaxxer.hikari.HikariDataSource
import io.kraftverk.core.module.Module
import io.kraftverk.core.module.port
import io.kraftverk.core.module.string
import io.realworld.app.domain.repository.ArticleRepository
import io.realworld.app.domain.repository.CommentRepository
import io.realworld.app.domain.repository.TagRepository
import io.realworld.app.domain.repository.UserRepository
import io.realworld.app.domain.service.ArticleService
import io.realworld.app.domain.service.CommentService
import io.realworld.app.domain.service.TagService
import io.realworld.app.domain.service.UserService
import io.realworld.app.utils.JwtService
import io.realworld.app.web.controllers.*
import io.realworld.app.web.server.Authorizer
import io.realworld.app.web.server.route
import io.realworld.app.web.server.server
import io.realworld.app.web.server.swagger
import org.h2.tools.Server

class AppModule0 : Module() {

    // Http
    val context by string("http.context")
    val port by port("http.port")

    val authorizer by bean { Authorizer(jwtService()) }

    val server by server(authorizer, context, port) {
        swagger("api.yaml")
        route(userController())
        route(profileController())
        route(articleController(), commentController())
        route(tagController())
    }

    // User beans
    val userController by bean { UserController(userService()) }
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
        configure(dataSource) {
            it.jdbcUrl = url()
            it.username = username()
            it.password = password()
        }
        configure(server) {
            lifecycle {
                onCreate { it.start() }
                onDestroy { it.stop() }
            }
        }
        configure(h2Server) {
            lifecycle {
                onCreate { it.start() }
                onDestroy { it.stop() }
            }
        }
    }

}
