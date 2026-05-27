package com.example.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateSeatRequest(
    val clubId: Int,
    val typeId: Int,
    val name: String,
    val type: String,
    val pricePerHour: Double
)

@Serializable
data class SeatLayoutResponse(
    val id: String,
    val label: String,
    val room: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val color: String,
    val displayText: String
)
