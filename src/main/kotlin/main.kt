package com.example

import com.example.data.repository.BookingRepository
import com.example.data.repository.SeatRepository
import com.example.data.repository.UserRepository
import com.example.domain.error.NotFoundException
import com.example.domain.service.AuthService
import com.example.domain.service.BookingService
import com.example.domain.service.JwtTokenService
import com.example.domain.service.PasswordHasher
import com.example.domain.service.SeatService
import com.example.plugins.configureErrorHandling
import com.example.plugins.configureExposed
import com.example.plugins.configureSecurity
import com.example.plugins.configureSerialization
import com.example.presentation.routing.authRoutes
import com.example.presentation.routing.bookingRoutes
import com.example.presentation.routing.seatRoutes
import com.example.presentation.routing.userId
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureErrorHandling()
    configureSecurity()
    configureExposed()

    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val expiresIn = environment.config.property("jwt.expiresInMs").getString().toLong()

    val userRepository = UserRepository()
    val seatRepository = SeatRepository()
    val bookingRepository = BookingRepository()
    val tokenService = JwtTokenService(secret, issuer, audience, expiresIn)
    val authService = AuthService(userRepository, PasswordHasher(), tokenService)
    val seatService = SeatService(seatRepository)
    val bookingService = BookingService(bookingRepository, seatRepository)

    routing {
        get("/") {
            call.respond(mapOf("status" to "Computer club server is running"))
        }

        authRoutes(authService)
        seatRoutes(seatService)
        bookingRoutes(bookingService)

        authenticate("auth-jwt") {
            get("/profile") {
                val userId = call.userId()
                val user = userRepository.findById(userId)
                    ?: throw NotFoundException("Пользователь не найден")
                call.respond(user)
            }
        }
    }
}
