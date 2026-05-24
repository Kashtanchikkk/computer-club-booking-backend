package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Seat(
    val id: Int,
    val name: String,
    val type: String,
    val pricePerHour: Double,
    val isActive: Boolean
)
