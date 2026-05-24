package com.example.presentation.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.principal
import io.ktor.server.response.*

fun ApplicationCall.userId(): Int =
    principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()

fun ApplicationCall.userRole(): String =
    principal<JWTPrincipal>()!!.payload.getClaim("role").asString()

suspend fun ApplicationCall.requireAdmin(): Boolean {
    if (userRole() != "admin") {
        respond(HttpStatusCode.Forbidden, "Доступ запрещён")
        return false
    }
    return true
}
