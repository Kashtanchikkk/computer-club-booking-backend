package com.example.data.repository

import com.example.data.db.GamingSeatsTable
import com.example.data.db.SeatTypesTable
import com.example.domain.model.Seat
import com.example.domain.model.SeatType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class SeatRepository {

    fun findTypes(): List<SeatType> = transaction {
        val activeTypeIds = GamingSeatsTable
            .selectAll()
            .where { GamingSeatsTable.isActive eq true }
            .map { it[GamingSeatsTable.typeId] }
            .toSet()

        if (activeTypeIds.isEmpty()) return@transaction emptyList()

        SeatTypesTable
            .selectAll()
            .where { SeatTypesTable.id inList activeTypeIds }
            .orderBy(SeatTypesTable.id to SortOrder.ASC)
            .map { it.toSeatType() }
    }

    fun findAll(clubId: Int? = null, typeId: Int? = null): List<Seat> = transaction {
        seatsWithTypes()
            .selectAll()
            .where {
                var condition: Op<Boolean> = GamingSeatsTable.isActive eq true
                if (clubId != null) condition = condition and (GamingSeatsTable.clubId eq clubId)
                if (typeId != null) condition = condition and (GamingSeatsTable.typeId eq typeId)
                condition
            }
            .map { it.toSeat() }
    }

    fun findActiveById(id: Int): Seat? = transaction {
        seatsWithTypes()
            .selectAll()
            .where { (GamingSeatsTable.id eq id) and (GamingSeatsTable.isActive eq true) }
            .map { it.toSeat() }
            .singleOrNull()
    }

    fun create(
        clubId: Int,
        typeId: Int,
        name: String,
        type: String,
        pricePerHour: Double
    ): Seat = transaction {
        val id = GamingSeatsTable.insert {
            it[GamingSeatsTable.clubId] = clubId
            it[GamingSeatsTable.typeId] = typeId
            it[GamingSeatsTable.name] = name
            it[isActive] = true
        }[GamingSeatsTable.id]

        val seatType = SeatTypesTable
            .selectAll()
            .where { SeatTypesTable.id eq typeId }
            .singleOrNull()

        Seat(
            id = id,
            clubId = clubId,
            typeId = typeId,
            name = name,
            type = seatType?.get(SeatTypesTable.name) ?: type,
            pricePerHour = seatType?.get(SeatTypesTable.pricePerHour)?.toDouble() ?: pricePerHour,
            processor = seatType?.get(SeatTypesTable.processor).orEmpty(),
            gpu = seatType?.get(SeatTypesTable.gpu).orEmpty(),
            ram = seatType?.get(SeatTypesTable.ram).orEmpty(),
            monitor = seatType?.get(SeatTypesTable.monitor).orEmpty(),
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
        clubId = this[GamingSeatsTable.clubId],
        typeId = this[GamingSeatsTable.typeId],
        name = this[GamingSeatsTable.name],
        type = this[SeatTypesTable.name],
        pricePerHour = this[SeatTypesTable.pricePerHour].toDouble(),
        processor = this[SeatTypesTable.processor],
        gpu = this[SeatTypesTable.gpu],
        ram = this[SeatTypesTable.ram],
        monitor = this[SeatTypesTable.monitor],
        isActive = this[GamingSeatsTable.isActive]
    )

    private fun ResultRow.toSeatType() = SeatType(
        id = this[SeatTypesTable.id],
        name = this[SeatTypesTable.name],
        pricePerHour = this[SeatTypesTable.pricePerHour].toDouble(),
        processor = this[SeatTypesTable.processor],
        gpu = this[SeatTypesTable.gpu],
        ram = this[SeatTypesTable.ram],
        monitor = this[SeatTypesTable.monitor]
    )

    private fun seatsWithTypes(): Join =
        GamingSeatsTable.join(
            otherTable = SeatTypesTable,
            joinType = JoinType.INNER,
            onColumn = GamingSeatsTable.typeId,
            otherColumn = SeatTypesTable.id
        )
}
