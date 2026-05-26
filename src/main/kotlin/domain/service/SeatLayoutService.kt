package com.example.domain.service

import com.example.data.repository.SeatLayoutRepository
import com.example.presentation.dto.SeatLayoutResponse

class SeatLayoutService(
    private val seatLayoutRepository: SeatLayoutRepository
) {
    fun getAllLayouts(): List<SeatLayoutResponse> =
        seatLayoutRepository.getAll()
}
