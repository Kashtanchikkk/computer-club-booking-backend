package com.example.presentation.routing

import com.example.domain.service.SeatService
import com.example.presentation.dto.CreateSeatRequest
import com.example.presentation.dto.UpdateSeatNameRequest
import com.example.presentation.dto.UpdateSeatStatusRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.seatRoutes(seatService: SeatService) {

    authenticate("auth-jwt") {

        get("/seats") {
            val clubId = call.request.queryParameters["clubId"]?.toIntOrNull()
            val typeId = call.request.queryParameters["typeId"]?.toIntOrNull()
            call.respond(seatService.findAll(clubId, typeId))
        }

        get("/seat-types") {
            call.respond(seatService.findTypes())
        }

        get("/seats/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Неверный id")
            call.respond(seatService.findById(id))
        }

        post("/admin/seats") {
            if (!call.requireAdmin()) return@post
            val request = call.receive<CreateSeatRequest>()
            val seat = seatService.create(request)
            call.respond(HttpStatusCode.Created, seat)
        }

        delete("/admin/seats/{id}") {
            if (!call.requireAdmin()) return@delete
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Неверный id")
            seatService.deactivate(id)
            call.respond(HttpStatusCode.OK)
        }

        patch("/admin/seats/{id}/name") {
            if (!call.requireAdmin()) return@patch
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, "Неверный id")
            val request = call.receive<UpdateSeatNameRequest>()
            call.respond(seatService.updateName(id, request))
        }

        patch("/admin/seats/{id}/status") {
            if (!call.requireAdmin()) return@patch
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, "Неверный id")
            val request = call.receive<UpdateSeatStatusRequest>()
            call.respond(seatService.updateStatus(id, request))
        }
    }
}
