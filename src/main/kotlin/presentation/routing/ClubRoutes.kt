package com.example.presentation.routing

import com.example.domain.service.ClubService
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.clubRoutes(clubService: ClubService) {
    authenticate("auth-jwt") {
        get("/clubs") {
            call.respond(clubService.findAll())
        }
    }
}
