package io.realworld.app.web

import com.auth0.jwt.exceptions.JWTVerificationException
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.*
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.util.SwaggerRenderer
import io.javalin.json.JavalinJackson
import io.javalin.security.SecurityUtil
import io.realworld.app.config.Roles
import io.realworld.app.web.controllers.*
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.text.SimpleDateFormat

class Gateway(
        private val controllers: Controllers,
        authService: AuthService,
        contextPath: String,
        port: Int
) {

    private val javalin = Javalin.create().apply {
        enableCorsForAllOrigins().contextPath(contextPath)
        accessManager { handler, ctx, permittedRoles ->
            if (authService.authorize(ctx, permittedRoles)) {
                handler.handle(ctx)
            } else throw ForbiddenResponse()
        }
        registerExceptionHandlers()
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

    fun start() = controllers.apply {
        val rolesOptionalAuthenticated = SecurityUtil.roles(Roles.ANYONE, Roles.AUTHENTICATED)
        javalin.routes {
            path("users") {
                post(userController::register, SecurityUtil.roles(Roles.ANYONE))
                post("login", userController::login, SecurityUtil.roles(Roles.ANYONE))
            }
            path("user") {
                get(userController::getCurrent, SecurityUtil.roles(Roles.AUTHENTICATED))
                put(userController::update, SecurityUtil.roles(Roles.AUTHENTICATED))
            }
            path("profiles/:username") {
                get(profileController::get, rolesOptionalAuthenticated)
                path("follow") {
                    post(profileController::follow, SecurityUtil.roles(Roles.AUTHENTICATED))
                    delete(profileController::unfollow, SecurityUtil.roles(Roles.AUTHENTICATED))
                }
            }
            path("articles") {
                get("feed", articleController::feed, SecurityUtil.roles(Roles.AUTHENTICATED))
                path(":slug") {
                    path("comments") {
                        post(commentController::add, SecurityUtil.roles(Roles.AUTHENTICATED))
                        get(commentController::findBySlug, rolesOptionalAuthenticated)
                        delete(":id", commentController::delete, SecurityUtil.roles(Roles.AUTHENTICATED))
                    }
                    path("favorite") {
                        post(articleController::favorite, SecurityUtil.roles(Roles.AUTHENTICATED))
                        delete(articleController::unfavorite, SecurityUtil.roles(Roles.AUTHENTICATED))
                    }
                    get(articleController::get, rolesOptionalAuthenticated)
                    put(articleController::update, SecurityUtil.roles(Roles.AUTHENTICATED))
                    delete(articleController::delete, SecurityUtil.roles(Roles.AUTHENTICATED))
                }
                get(articleController::findBy, rolesOptionalAuthenticated)
                post(articleController::create, SecurityUtil.roles(Roles.AUTHENTICATED))
            }
            path("tags") {
                get(tagController::get, rolesOptionalAuthenticated)
            }
            get("", SwaggerRenderer("swagger/api.yaml"), rolesOptionalAuthenticated)
        }
        javalin.start()
    }

    fun stop(): Javalin? {
        return javalin.stop()
    }
}

class Controllers(
        val userController: UserController,
        val profileController: ProfileController,
        val articleController: ArticleController,
        val commentController: CommentController,
        val tagController: TagController
)

private fun Javalin.registerExceptionHandlers() {
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
}