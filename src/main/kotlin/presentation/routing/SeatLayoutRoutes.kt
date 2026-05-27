package com.example.presentation.routing

import com.example.domain.service.SeatLayoutService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.seatLayoutRoutes(seatLayoutService: SeatLayoutService) {
    get("/seat-layouts") {
        call.respond(seatLayoutService.getAllLayouts())
    }
}
