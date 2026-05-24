package com.example.domain.validation

import com.example.domain.error.BadRequestException

object SeatValidator {
    fun validateCreate(name: String, type: String, pricePerHour: Double) {
        if (name.trim().length < 2) {
            throw BadRequestException("Название места должно содержать минимум 2 символа")
        }
        if (type.trim().length < 2) {
            throw BadRequestException("Тип места должен содержать минимум 2 символа")
        }
        if (pricePerHour <= 0.0) {
            throw BadRequestException("Цена за час должна быть больше 0")
        }
    }
}
