package io.realworld.app.config

import io.javalin.security.Role

internal enum class Roles : Role {
    ANYONE, AUTHENTICATED
}
