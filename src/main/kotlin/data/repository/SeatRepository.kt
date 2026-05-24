package com.example.data.repository

import com.example.data.db.GamingSeatsTable
import com.example.domain.model.Seat
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class SeatRepository {

    fun findAll(): List<Seat> = transaction {
        GamingSeatsTable
            .selectAll()
            .where { GamingSeatsTable.isActive eq true }
            .map { it.toSeat() }
    }

    fun findActiveById(id: Int): Seat? = transaction {
        GamingSeatsTable
            .selectAll()
            .where { (GamingSeatsTable.id eq id) and (GamingSeatsTable.isActive eq true) }
            .map { it.toSeat() }
            .singleOrNull()
    }

    fun create(name: String, type: String, pricePerHour: Double): Seat = transaction {
        val id = GamingSeatsTable.insert {
            it[GamingSeatsTable.name] = name
            it[GamingSeatsTable.type] = type
            it[GamingSeatsTable.pricePerHour] = pricePerHour.toBigDecimal()
            it[isActive] = true
        }[GamingSeatsTable.id]

        Seat(
            id = id,
            name = name,
            type = type,
            pricePerHour = pricePerHour,
            isActive = true
        )
    }

    fun deactivate(id: Int): Boolean = transaction {
        GamingSeatsTable.update({ GamingSeatsTable.id eq id }) {
            it[isActive] = false
        } > 0
    }

    private fun ResultRow.toSeat() = Seat(
        id = this[GamingSeatsTable.id],
        name = this[GamingSeatsTable.name],
        type = this[GamingSeatsTable.type],
        pricePerHour = this[GamingSeatsTable.pricePerHour].toDouble(),
        isActive = this[GamingSeatsTable.isActive]
    )
}
