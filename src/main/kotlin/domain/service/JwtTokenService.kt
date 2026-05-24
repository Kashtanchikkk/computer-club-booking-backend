package com.example.domain.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class JwtTokenService(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
    private val expiresIn: Long
) {
    fun generate(userId: Int, role: String): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withClaim("role", role)
        .withExpiresAt(Date(System.currentTimeMillis() + expiresIn))
        .sign(Algorithm.HMAC256(secret))
}
