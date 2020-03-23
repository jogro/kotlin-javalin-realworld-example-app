package io.realworld.app

import com.zaxxer.hikari.HikariConfig
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
import io.realworld.app.web.server.Authorizer
import io.realworld.app.web.controllers.*
import io.realworld.app.web.server.route
import io.realworld.app.web.server.server
import io.realworld.app.web.server.swagger
import org.h2.tools.Server

class AppModule2 : Module() {
    val http by module { HttpModule() }
    val ctrl by module { ControllerModule() }
    val srv by module { ServiceModule() }
    val repo by module { RepositoryModule() }
    val common by module { CommonModule() }
    val jdbc by module { JdbcModule() }
}

class HttpModule : ModuleOf<AppModule2>() {

    val ctrl by import { ctrl }
    val jwtService by ref { common.jwtService }

    val port by port()
    val context by string()

    val authorizer by bean { Authorizer(jwtService()) }

    val server by server(authorizer, context, port) {
        with(ctrl()) {
            swagger("api.yaml")
            route(userController())
            route(profileController())
            route(articleController(), commentController())
            route(tagController())
        }
    }

    init {
        onCreate(server) { it.start() }
    }
}

class ControllerModule : ModuleOf<AppModule2>() {

    val srv by import { srv }

    val userController by bean { UserController(srv().userService()) }
    val articleController by bean { ArticleController(srv().articleService()) }
    val profileController by bean { ProfileController(srv().userService()) }
    val commentController by bean { CommentController(srv().commentService()) }
    val tagController by bean { TagController(srv().tagService()) }
}

class ServiceModule : ModuleOf<AppModule2>() {

    val repo by import { repo }

    val jwtService by ref { common.jwtService }

    val userService by bean { UserService(jwtService(), repo().userRepository()) }
    val articleService by bean { ArticleService(repo().articleRepository(), repo().userRepository()) }
    val commentService by bean { CommentService(repo().commentRepository()) }
    val tagService by bean { TagService(repo().tagRepository()) }
}

class RepositoryModule : ModuleOf<AppModule2>() {

    val dataSource by ref { jdbc.dataSource }

    val userRepository by bean { UserRepository(dataSource()) }
    val articleRepository by bean { ArticleRepository(dataSource()) }
    val commentRepository by bean { CommentRepository(dataSource()) }
    val tagRepository by bean { TagRepository(dataSource()) }
}

class CommonModule : ModuleOf<AppModule2>() {
    val jwtService by bean { JwtService() }
}

class JdbcModule : Module() {

    val url by string()
    val username by string()
    val password by string(secret = true)
    val config by bean { HikariConfig() }
    val dataSource by bean { HikariDataSource(config()) }
    val h2Server by bean { Server.createWebServer() }

    init {
        customize(config) { c ->
            c.jdbcUrl = url()
            c.username = username()
            c.password = password()
        }
        onCreate(h2Server) { it.start() }
        onDestroy(h2Server) { it.stop() }
    }
}
