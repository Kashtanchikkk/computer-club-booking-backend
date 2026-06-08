package com.example.domain.validation

import com.example.domain.error.BadRequestException
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

object BookingValidator {
    fun parseBookingPeriod(startTime: String, endTime: String): Pair<LocalDateTime, LocalDateTime> {
        val start = parseDateTime(startTime, "Некорректное время начала")
        val end = parseDateTime(endTime, "Некорректное время окончания")

        if (!end.isAfter(start)) {
            throw BadRequestException("Время окончания должно быть позже времени начала")
        }

        if (start.isBefore(LocalDateTime.now())) {
            throw BadRequestException("Нельзя создать бронь в прошлом")
        }

        return start to end
    }

    private fun parseDateTime(value: String, message: String): LocalDateTime =
        try {
            LocalDateTime.parse(value)
        } catch (_: DateTimeParseException) {
            throw BadRequestException(message)
        }
}
