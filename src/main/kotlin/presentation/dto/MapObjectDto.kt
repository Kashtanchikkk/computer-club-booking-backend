package com.example.presentation.dto

import com.example.domain.model.MapObject
import com.example.domain.model.MapObjectType
import kotlinx.serialization.Serializable

@Serializable
data class MapObjectDto(
    val id: Long,
    val type: MapObjectType,
    val title: String? = null,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val seatId: Long? = null
)

fun MapObject.toDto() = MapObjectDto(
    id = id,
    type = type,
    title = title,
    x = x,
    y = y,
    width = width,
    height = height,
    seatId = seatId
)
