package com.example.data.repository

import com.example.data.db.ComputerClubsTable
import com.example.domain.model.ComputerClub
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ClubRepository {
    fun findAll(): List<ComputerClub> = transaction {
        ComputerClubsTable
            .selectAll()
            .map { it.toComputerClub() }
    }

    private fun ResultRow.toComputerClub() = ComputerClub(
        id = this[ComputerClubsTable.id],
        name = this[ComputerClubsTable.name],
        address = this[ComputerClubsTable.address],
        rating = this[ComputerClubsTable.rating].toDouble()
    )
}
