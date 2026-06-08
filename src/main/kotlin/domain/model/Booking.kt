package com.example.domain.model

import kotlinx.serialization.Serializable

enum class BookingStatus(val value: String) {
    Pending("pending"),
    Confirmed("confirmed"),
    Cancelled("cancelled")
}

@Serializable
data class Booking(
    val id: Int,
    val userId: Int,
    val seatId: Int,
    val seatName: String,
    val startTime: String,
    val endTime: String,
    val status: String,
    val totalPrice: Double
)
