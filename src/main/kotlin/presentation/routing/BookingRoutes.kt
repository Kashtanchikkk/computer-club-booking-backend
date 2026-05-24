package com.example.presentation.routing

import com.example.domain.service.BookingService
import com.example.presentation.dto.CreateBookingRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.bookingRoutes(bookingService: BookingService) {
    authenticate("auth-jwt") {

        post("/bookings") {
            val userId = call.userId()
            val request = call.receive<CreateBookingRequest>()
            val booking = bookingService.createBooking(userId, request)
            call.respond(HttpStatusCode.Created, booking)
        }

        get("/bookings/my") {
            val userId = call.userId()
            call.respond(bookingService.findUserBookings(userId))
        }

        delete("/bookings/{id}") {
            val userId = call.userId()
            val role = call.userRole()
            val bookingId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Неверный id")
            bookingService.cancelBooking(bookingId, userId, role)
            call.respond(HttpStatusCode.OK)
        }

        get("/admin/bookings") {
            if (!call.requireAdmin()) return@get
            call.respond(bookingService.findAllBookings())
        }
    }
}
