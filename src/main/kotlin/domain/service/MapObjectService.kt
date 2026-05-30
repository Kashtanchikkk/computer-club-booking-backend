package com.example.domain.service

import com.example.data.repository.MapObjectRepository
import com.example.presentation.dto.MapObjectDto
import com.example.presentation.dto.toDto

class MapObjectService(
    private val mapObjectRepository: MapObjectRepository
) {
    fun getClubMap(clubId: Long): List<MapObjectDto> =
        mapObjectRepository.findByClubId(clubId).map { it.toDto() }
}
