package com.example.domain.service

import com.example.data.repository.UserRepository
import com.example.domain.error.ConflictException
import com.example.domain.error.UnauthorizedException
import com.example.domain.validation.AuthValidator
import com.example.presentation.dto.AuthResponse
import com.example.presentation.dto.LoginRequest
import com.example.presentation.dto.RefreshTokenRequest
import com.example.presentation.dto.RegisterRequest

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtTokenService: JwtTokenService
) {
    fun register(request: RegisterRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        val name = request.name.trim()

        AuthValidator.validateRegister(email, request.password, name)

        if (userRepository.findByEmail(email) != null) {
            throw ConflictException("Email уже занят")
        }

        val user = userRepository.create(
            email = email,
            passwordHash = passwordHasher.hash(request.password),
            name = name
        )
        return authResponse(user)
    }

    fun login(request: LoginRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        AuthValidator.validateLogin(email, request.password)

        val authData = userRepository.findAuthDataByEmail(email)
            ?: throw UnauthorizedException("Неверный email или пароль")

        if (!passwordHasher.verify(request.password, authData.passwordHash)) {
            throw UnauthorizedException("Неверный email или пароль")
        }

        return authResponse(authData.user)
    }

    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val payload = jwtTokenService.verifyRefresh(request.refreshToken)
            ?: throw UnauthorizedException("Сессия истекла, войдите заново")

        val user = userRepository.findById(payload.userId)
            ?: throw UnauthorizedException("Пользователь не найден")

        return authResponse(user)
    }

    private fun authResponse(user: com.example.domain.model.User): AuthResponse {
        val accessToken = jwtTokenService.generateAccess(user.id, user.role)
        val refreshToken = jwtTokenService.generateRefresh(user.id, user.role)
        return AuthResponse(
            token = accessToken,
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user
        )
    }
}
