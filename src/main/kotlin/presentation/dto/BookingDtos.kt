package com.example.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateBookingRequest(
    val seatId: Int,
    val startTime: String,
    val endTime: String
)
