package com.example.domain.service

import com.example.data.repository.SeatRepository
import com.example.domain.error.NotFoundException
import com.example.domain.model.Seat
import com.example.domain.model.SeatType
import com.example.domain.validation.SeatValidator
import com.example.presentation.dto.CreateSeatRequest

class SeatService(
    private val seatRepository: SeatRepository
) {
    fun findTypes(): List<SeatType> =
        seatRepository.findTypes()

    fun findAll(clubId: Int? = null, typeId: Int? = null): List<Seat> =
        seatRepository.findAll(clubId, typeId)

    fun findById(id: Int): Seat =
        seatRepository.findActiveById(id) ?: throw NotFoundException("Место не найдено")

    fun create(request: CreateSeatRequest): Seat {
        val name = request.name.trim()
        val type = request.type.trim()
        SeatValidator.validateCreate(name, type, request.pricePerHour)
        return seatRepository.create(
            clubId = request.clubId,
            typeId = request.typeId,
            name = name,
            type = type,
            pricePerHour = request.pricePerHour
        )
    }

    fun deactivate(id: Int) {
        val deactivated = seatRepository.deactivate(id)
        if (!deactivated) {
            throw NotFoundException("Место не найдено")
        }
    }
}
