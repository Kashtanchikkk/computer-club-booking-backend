package com.example.presentation.dto

import com.example.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val accessToken: String,
    val refreshToken: String,
    val user: User
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)
