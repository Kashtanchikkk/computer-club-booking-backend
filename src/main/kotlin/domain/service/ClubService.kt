package com.example.domain.service

import com.example.data.repository.ClubRepository
import com.example.domain.model.ComputerClub

class ClubService(
    private val clubRepository: ClubRepository
) {
    fun findAll(): List<ComputerClub> =
        clubRepository.findAll()
}
