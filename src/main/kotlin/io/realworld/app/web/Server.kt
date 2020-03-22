package io.realworld.app.web

import com.auth0.jwt.exceptions.JWTVerificationException
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.*
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.util.SwaggerRenderer
import io.javalin.json.JavalinJackson
import io.javalin.security.SecurityUtil.roles
import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.declaration.BeanDeclaration
import io.kraftverk.declaration.CustomBeanDeclaration
import io.kraftverk.module.AbstractModule
import io.kraftverk.module.bean
import io.realworld.app.config.Roles.ANYONE
import io.realworld.app.config.Roles.AUTHENTICATED
import io.realworld.app.web.controllers.*
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.text.SimpleDateFormat

class Server(private val javalin: Javalin) {
    fun start() = javalin.start()
    fun stop() = javalin.stop()
}

fun AbstractModule.server(authService: Bean<AuthService>,
                          contextPath: Value<String>,
                          port: Value<Int>,
                          block: ServerDeclaration.() -> Unit) = bean {
    val javalin = createJavalin(authService(), contextPath(), port())
    val definition = ServerDeclaration(javalin, this)
    definition.block()
    Server(javalin)
}

val rolesOptionalAuthenticated = roles(ANYONE, AUTHENTICATED)

class ServerDeclaration(internal val javalin: Javalin, parent: BeanDeclaration) : CustomBeanDeclaration(parent)

fun ServerDeclaration.route(userController: UserController) = javalin.routes {
    path("users") {
        post(userController::register, roles(ANYONE))
        post("login", userController::login, roles(ANYONE))
    }
    path("user") {
        get(userController::getCurrent, roles(AUTHENTICATED))
        put(userController::update, roles(AUTHENTICATED))
    }
}

fun ServerDeclaration.route(profileController: ProfileController) = javalin.routes {
    path("profiles/:username") {
        get(profileController::get, rolesOptionalAuthenticated)
        path("follow") {
            post(profileController::follow, roles(AUTHENTICATED))
            delete(profileController::unfollow, roles(AUTHENTICATED))
        }
    }
}

fun ServerDeclaration.route(articleController: ArticleController, commentController: CommentController) = javalin.routes {
    path("articles") {
        get("feed", articleController::feed, roles(AUTHENTICATED))
        path(":slug") {
            path("comments") {
                post(commentController::add, roles(AUTHENTICATED))
                get(commentController::findBySlug, rolesOptionalAuthenticated)
                delete(":id", commentController::delete, roles(AUTHENTICATED))
            }
            path("favorite") {
                post(articleController::favorite, roles(AUTHENTICATED))
                delete(articleController::unfavorite, roles(AUTHENTICATED))
            }
            get(articleController::get, rolesOptionalAuthenticated)
            put(articleController::update, roles(AUTHENTICATED))
            delete(articleController::delete, roles(AUTHENTICATED))
        }
        get(articleController::findBy, rolesOptionalAuthenticated)
        post(articleController::create, roles(AUTHENTICATED))
    }
}

fun ServerDeclaration.route(tagController: TagController) = javalin.routes {
    path("tags") {
        get(tagController::get, rolesOptionalAuthenticated)
    }
}

internal data class ErrorResponse(val errors: Map<String, List<String?>>)

private fun createJavalin(authService: AuthService, contextPath: String, port: Int): Javalin {
    return Javalin.create().apply {
        enableCorsForAllOrigins().contextPath(contextPath)
        accessManager { handler, ctx, permittedRoles ->
            if (authService.authorize(ctx, permittedRoles)) {
                handler.handle(ctx)
            } else throw ForbiddenResponse()
        }
        routes {
            ApiBuilder.get("", SwaggerRenderer("swagger/api.yaml"), rolesOptionalAuthenticated)
        }
        exception(Exception::class.java) { e, ctx ->
            //LOG.error("Exception occurred for req -> ${ctx.url()}", e)
            val error = ErrorResponse(mapOf("Unknown Error" to listOf(e.message
                    ?: "Error occurred!")))
            ctx.json(error).status(HttpStatus.INTERNAL_SERVER_ERROR_500)
        }
        exception(ExposedSQLException::class.java) { _, ctx ->
            //LOG.error("Exception occurred for req -> ${ctx.url()}", e)
            val error = ErrorResponse(mapOf("Unknown Error" to listOf("Error occurred!")))
            ctx.json(error).status(HttpStatus.INTERNAL_SERVER_ERROR_500)
        }
        exception(BadRequestResponse::class.java) { _, ctx ->
            //LOG.warn("BadRequestResponse occurred for req -> ${ctx.url()}")
            val error = ErrorResponse(mapOf("body" to listOf("can't be empty or invalid")))
            ctx.json(error).status(HttpStatus.UNPROCESSABLE_ENTITY_422)
        }
        exception(UnauthorizedResponse::class.java) { _, ctx ->
            //LOG.warn("UnauthorizedResponse occurred for req -> ${ctx.url()}")
            val error = ErrorResponse(mapOf("login" to listOf("User not authenticated!")))
            ctx.json(error).status(HttpStatus.UNAUTHORIZED_401)
        }
        exception(ForbiddenResponse::class.java) { _, ctx ->
            //LOG.warn("ForbiddenResponse occurred for req -> ${ctx.url()}")
            val error = ErrorResponse(mapOf("login" to listOf("User doesn't have permissions to perform the action!")))
            ctx.json(error).status(HttpStatus.FORBIDDEN_403)
        }
        exception(JWTVerificationException::class.java) { e, ctx ->
            //LOG.error("JWTVerificationException occurred for req -> ${ctx.url()}", e)
            val error = ErrorResponse(mapOf("token" to listOf(e.message
                    ?: "Invalid JWT token!")))
            ctx.json(error).status(HttpStatus.UNAUTHORIZED_401)
        }
        exception(NotFoundResponse::class.java) { _, ctx ->
            //LOG.warn("NotFoundResponse occurred for req -> ${ctx.url()}")
            val error = ErrorResponse(mapOf("body" to listOf("Resource can't be found to fulfill the request.")))
            ctx.json(error).status(HttpStatus.NOT_FOUND_404)
        }
        exception(HttpResponseException::class.java) { e, ctx ->
            //LOG.warn("HttpResponseException occurred for req -> ${ctx.url()}")
            val error = ErrorResponse(mapOf("body" to listOf(e.message)))
            ctx.json(error).status(e.status)
        }
        port(port)
        enableWebJars()
    }.also {
        JavalinJackson.configure(
                jacksonObjectMapper()
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .setDateFormat(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                        .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
        )
    }
}
