package com.example.domain.service

import com.example.data.repository.UserRepository
import com.example.domain.error.ConflictException
import com.example.domain.error.UnauthorizedException
import com.example.domain.validation.AuthValidator
import com.example.presentation.dto.AuthResponse
import com.example.presentation.dto.LoginRequest
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
        return AuthResponse(jwtTokenService.generate(user.id, user.role), user)
    }

    fun login(request: LoginRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        AuthValidator.validateLogin(email, request.password)

        val authData = userRepository.findAuthDataByEmail(email)
            ?: throw UnauthorizedException("Неверный email или пароль")

        if (!passwordHasher.verify(request.password, authData.passwordHash)) {
            throw UnauthorizedException("Неверный email или пароль")
        }

        return AuthResponse(jwtTokenService.generate(authData.user.id, authData.user.role), authData.user)
    }
}
