package com.example.domain.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.util.Date

class JwtTokenService(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
    private val accessExpiresIn: Long,
    private val refreshExpiresIn: Long
) {
    private val algorithm = Algorithm.HMAC256(secret)
    private val verifier = JWT.require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    fun generateAccess(userId: Int, role: String): String =
        generate(userId = userId, role = role, type = ACCESS_TYPE, expiresIn = accessExpiresIn)

    fun generateRefresh(userId: Int, role: String): String =
        generate(userId = userId, role = role, type = REFRESH_TYPE, expiresIn = refreshExpiresIn)

    fun verifyRefresh(token: String): RefreshTokenPayload? = try {
        val decoded = verifier.verify(token)
        val type = decoded.getClaim("type").asString()
        val userId = decoded.getClaim("userId").asInt()
        val role = decoded.getClaim("role").asString()
        if (type == REFRESH_TYPE && userId != null && role != null) {
            RefreshTokenPayload(userId = userId, role = role)
        } else {
            null
        }
    } catch (_: JWTVerificationException) {
        null
    }

    private fun generate(userId: Int, role: String, type: String, expiresIn: Long): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withClaim("role", role)
        .withClaim("type", type)
        .withExpiresAt(Date(System.currentTimeMillis() + expiresIn))
        .sign(algorithm)

    private companion object {
        const val ACCESS_TYPE = "access"
        const val REFRESH_TYPE = "refresh"
    }
}

data class RefreshTokenPayload(
    val userId: Int,
    val role: String
)
