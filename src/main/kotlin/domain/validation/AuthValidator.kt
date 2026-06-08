package com.example.domain.validation

import com.example.domain.error.BadRequestException

object AuthValidator {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    fun validateRegister(email: String, password: String, name: String) {
        validateEmail(email)
        validatePassword(password)
        if (name.trim().length < 2) {
            throw BadRequestException("Имя должно содержать минимум 2 символа")
        }
    }

    fun validateLogin(email: String, password: String) {
        validateEmail(email)
        if (password.isBlank()) {
            throw BadRequestException("Пароль не может быть пустым")
        }
    }

    private fun validateEmail(email: String) {
        if (!emailRegex.matches(email.trim())) {
            throw BadRequestException("Некорректный email")
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw BadRequestException("Пароль должен содержать минимум 8 символов")
        }
        if (!password.any(Char::isDigit) || !password.any(Char::isLetter)) {
            throw BadRequestException("Пароль должен содержать буквы и цифры")
        }
    }
}
