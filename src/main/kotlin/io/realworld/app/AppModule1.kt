package io.realworld.app

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kraftverk.core.module.Module
import io.kraftverk.core.module.module
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

class AppModule1 : Module() {

    val http by module { HttpModule() }
    val ctrl by module { ControllerModule() }
    val srv by module { ServiceModule() }
    val repo by module { RepositoryModule() }
    val jdbc by module { JdbcModule() }
    val common by module { CommonModule() }

    inner class HttpModule : Module() {

        val port by port()
        val context by string()

        val authorizer by bean { Authorizer(common.jwtService()) }

        val server by server(authorizer, context, port) {
            with(ctrl) {
                swagger("api.yaml")
                route(userController())
                route(profileController())
                route(articleController(), ctrl.commentController())
                route(tagController())
            }
        }

        init {
            configure(server) {
                lifecycle {
                    onCreate { it.start() }
                    onDestroy { it.stop() }
                }

            }
        }
    }

    inner class ControllerModule : Module() {
        val userController by bean { UserController(srv.userService()) }
        val articleController by bean { ArticleController(srv.articleService()) }
        val profileController by bean { ProfileController(srv.userService()) }
        val commentController by bean { CommentController(srv.commentService()) }
        val tagController by bean { TagController(srv.tagService()) }
    }

    inner class ServiceModule : Module() {
        val userService by bean { UserService(common.jwtService(), repo.userRepository()) }
        val articleService by bean { ArticleService(repo.articleRepository(), repo.userRepository()) }
        val commentService by bean { CommentService(repo.commentRepository()) }
        val tagService by bean { TagService(repo.tagRepository()) }
    }

    inner class RepositoryModule : Module() {
        val userRepository by bean { UserRepository(jdbc.dataSource()) }
        val articleRepository by bean { ArticleRepository(jdbc.dataSource()) }
        val commentRepository by bean { CommentRepository(jdbc.dataSource()) }
        val tagRepository by bean { TagRepository(jdbc.dataSource()) }
    }

    class CommonModule : Module() {
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
            configure(config) {
                it.jdbcUrl = url()
                it.username = username()
                it.password = password()
            }
            configure(h2Server) {
                lifecycle {
                    onCreate { it.start() }
                    onDestroy { it.stop() }
                }
            }
        }
    }

}


