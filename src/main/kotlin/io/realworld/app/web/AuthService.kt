package io.realworld.app.web

import com.auth0.jwt.interfaces.DecodedJWT
import io.javalin.Context
import io.javalin.security.Role
import io.realworld.app.config.Roles
import io.realworld.app.utils.JwtService

class AuthService(private val jwtService: JwtService) {

    fun authorize(ctx: Context, permittedRoles: Set<Role>): Boolean {
        val authorizationToken = ctx.authorizationToken?.let(jwtService::decodeJWT)
        val userRole = authorizationToken?.role ?: Roles.ANYONE
        if (userRole in permittedRoles) {
            ctx.attribute("email", authorizationToken?.subject)
            return true
        }
        return false
    }

    private val Context.authorizationToken: String?
        get() = header("Authorization")
                ?.substringAfter("Token")
                ?.trim()

    private val DecodedJWT.role: Roles?
        get() = getClaim("role")
                ?.asString()
                ?.let(Roles::valueOf)
}
