package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Seat(
    val id: Int,
    val clubId: Int,
    val typeId: Int,
    val name: String,
    val type: String,
    val pricePerHour: Double,
    val processor: String,
    val gpu: String,
    val ram: String,
    val monitor: String,
    val isActive: Boolean
)

@Serializable
data class SeatType(
    val id: Int,
    val name: String,
    val pricePerHour: Double,
    val processor: String,
    val gpu: String,
    val ram: String,
    val monitor: String
)

@Serializable
data class ComputerClub(
    val id: Int,
    val name: String,
    val address: String,
    val rating: Double
)
