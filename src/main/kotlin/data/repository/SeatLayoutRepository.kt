package com.example.data.repository

import com.example.data.db.SeatLayoutsTable
import com.example.presentation.dto.SeatLayoutResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class SeatLayoutRepository {
    fun getAll(): List<SeatLayoutResponse> = transaction {
        SeatLayoutsTable
            .selectAll()
            .orderBy(SeatLayoutsTable.id to SortOrder.ASC)
            .map { it.toResponse() }
    }

    private fun ResultRow.toResponse() = SeatLayoutResponse(
        id = this[SeatLayoutsTable.id],
        label = this[SeatLayoutsTable.label],
        room = this[SeatLayoutsTable.room],
        x = this[SeatLayoutsTable.x],
        y = this[SeatLayoutsTable.y],
        width = this[SeatLayoutsTable.width],
        height = this[SeatLayoutsTable.height],
        color = this[SeatLayoutsTable.color],
        displayText = this[SeatLayoutsTable.displayText]
    )
}
