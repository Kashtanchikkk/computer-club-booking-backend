package com.example.presentation.routing

import com.example.domain.service.AuthService
import com.example.presentation.dto.LoginRequest
import com.example.presentation.dto.RefreshTokenRequest
import com.example.presentation.dto.RegisterRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    post("/auth/register") {
        val request = call.receive<RegisterRequest>()
        call.respond(HttpStatusCode.Created, authService.register(request))
    }

    post("/auth/login") {
        val request = call.receive<LoginRequest>()
        call.respond(authService.login(request))
    }

    post("/auth/refresh") {
        val request = call.receive<RefreshTokenRequest>()
        call.respond(authService.refresh(request))
    }
}
