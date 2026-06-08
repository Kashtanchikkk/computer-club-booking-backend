package com.example.domain.model

enum class MapObjectType {
    ROOM,
    WALL,
    SEAT
}

data class MapObject(
    val id: Long,
    val clubId: Long,
    val type: MapObjectType,
    val title: String?,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val seatId: Long? = null
)
