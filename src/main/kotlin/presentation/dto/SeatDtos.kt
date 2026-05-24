package com.example.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateSeatRequest(
    val name: String,
    val type: String,
    val pricePerHour: Double
)
