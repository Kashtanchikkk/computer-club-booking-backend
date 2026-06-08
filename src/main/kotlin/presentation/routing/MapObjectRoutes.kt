package com.example.presentation.routing

import com.example.domain.service.MapObjectService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.mapObjectRoutes(mapObjectService: MapObjectService) {
    authenticate("auth-jwt") {
        get("/clubs/{clubId}/map") {
            val clubId = call.parameters["clubId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Неверный clubId")

            call.respond(mapObjectService.getClubMap(clubId))
        }
    }
}
