package com.example.domain.error

import io.ktor.http.HttpStatusCode

open class AppException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String
) : RuntimeException(message)

class BadRequestException(message: String) : AppException(
    status = HttpStatusCode.BadRequest,
    code = "BAD_REQUEST",
    message = message
)

class UnauthorizedException(message: String) : AppException(
    status = HttpStatusCode.Unauthorized,
    code = "UNAUTHORIZED",
    message = message
)

class ForbiddenException(message: String) : AppException(
    status = HttpStatusCode.Forbidden,
    code = "FORBIDDEN",
    message = message
)

class NotFoundException(message: String) : AppException(
    status = HttpStatusCode.NotFound,
    code = "NOT_FOUND",
    message = message
)

class ConflictException(message: String) : AppException(
    status = HttpStatusCode.Conflict,
    code = "CONFLICT",
    message = message
)
