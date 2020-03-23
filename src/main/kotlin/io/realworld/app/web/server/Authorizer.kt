package io.realworld.app.web.server

import com.auth0.jwt.interfaces.DecodedJWT
import io.javalin.Context
import io.javalin.security.Role
import io.realworld.app.config.Roles
import io.realworld.app.utils.JwtService

class Authorizer(private val jwtService: JwtService) {

    fun authorize(ctx: Context, permittedRoles: Set<Role>): Boolean {
        val token = ctx.authToken?.let(jwtService::decodeJWT)?.also {
            ctx.attribute("email", it.subject)
        }
        return (token?.role ?: Roles.ANYONE) in permittedRoles
    }

    private val Context.authToken: String?
        get() = header("Authorization")
                ?.substringAfter("Token")
                ?.trim()

    private val DecodedJWT.role: Roles?
        get() = getClaim("role")
                ?.asString()
                ?.let(Roles::valueOf)
}
