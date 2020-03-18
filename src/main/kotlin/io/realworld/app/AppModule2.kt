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
import io.realworld.app.web.AuthService
import io.realworld.app.web.Controllers
import io.realworld.app.web.Gateway
import io.realworld.app.web.controllers.*
import org.h2.tools.Server

class AppModule2 : Module() {
    val http by partition { HttpModule() }
    val ctrl by partition { ControllerModule() }
    val srv by partition { ServiceModule() }
    val repo by partition { RepositoryModule() }
    val common by partition { CommonModule() }
    val jdbc by module { JdbcModule() }
}

class HttpModule : PartitionOf<AppModule2>() {

    val controllers by ref { ctrl.controllers }
    val jwtService by ref { common.jwtService }

    val port by port()
    val context by string()

    val authService by bean { AuthService(jwtService()) }
    val gateway by bean {
        Gateway(controllers(), authService(), context(), port())
    }

    init {
        onCreate(gateway) { it.start() }
    }
}

class ControllerModule : PartitionOf<AppModule2>() {

    val srv by import { srv }

    val controllers by bean {
        Controllers(userController(), profileController(), articleController(), commentController(), tagController())
    }
    val userController by bean { UserController(srv().userService()) }
    val articleController by bean { ArticleController(srv().articleService()) }
    val profileController by bean { ProfileController(srv().userService()) }
    val commentController by bean { CommentController(srv().commentService()) }
    val tagController by bean { TagController(srv().tagService()) }
}

class ServiceModule : PartitionOf<AppModule2>() {

    val repo by import { repo }

    val jwtService by ref { common.jwtService }

    val userService by bean { UserService(jwtService(), repo().userRepository()) }
    val articleService by bean { ArticleService(repo().articleRepository(), repo().userRepository()) }
    val commentService by bean { CommentService(repo().commentRepository()) }
    val tagService by bean { TagService(repo().tagRepository()) }
}

class RepositoryModule : PartitionOf<AppModule2>() {

    val dataSource by ref { jdbc.dataSource }

    val userRepository by bean { UserRepository(dataSource()) }
    val articleRepository by bean { ArticleRepository(dataSource()) }
    val commentRepository by bean { CommentRepository(dataSource()) }
    val tagRepository by bean { TagRepository(dataSource()) }
}

class CommonModule : PartitionOf<AppModule2>() {
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
