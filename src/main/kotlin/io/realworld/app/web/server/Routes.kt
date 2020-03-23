package io.realworld.app.web.server

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.util.SwaggerRenderer
import io.javalin.security.SecurityUtil.roles
import io.realworld.app.config.Roles.ANYONE
import io.realworld.app.config.Roles.AUTHENTICATED
import io.realworld.app.web.controllers.*

private val rolesOptionalAuthenticated = roles(ANYONE, AUTHENTICATED)

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

fun ServerDeclaration.swagger(file: String) = javalin.routes {
    get("", SwaggerRenderer("swagger/$file"), rolesOptionalAuthenticated)
}
