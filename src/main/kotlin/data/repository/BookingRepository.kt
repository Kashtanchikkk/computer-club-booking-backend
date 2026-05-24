package com.example.data.repository

import com.example.data.db.BookingsTable
import com.example.data.db.GamingSeatsTable
import com.example.domain.model.Booking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class BookingRepository {

    fun hasConflict(seatId: Int, start: LocalDateTime, end: LocalDateTime): Boolean = transaction {
        BookingsTable
            .selectAll()
            .where {
                (BookingsTable.seatId eq seatId) and
                        (BookingsTable.status neq "cancelled") and
                        (BookingsTable.startTime less end) and
                        (BookingsTable.endTime greater start)
            }
            .count() > 0
    }

    fun create(
        userId: Int,
        seatId: Int,
        seatName: String,
        start: LocalDateTime,
        end: LocalDateTime,
        totalPrice: Double
    ): Booking = transaction {
        val id = BookingsTable.insert {
            it[BookingsTable.userId] = userId
            it[BookingsTable.seatId] = seatId
            it[BookingsTable.startTime] = start
            it[BookingsTable.endTime] = end
            it[BookingsTable.status] = "confirmed"
            it[BookingsTable.totalPrice] = totalPrice.toBigDecimal()
            it[BookingsTable.createdAt] = LocalDateTime.now()
        }[BookingsTable.id]

        Booking(
            id = id,
            userId = userId,
            seatId = seatId,
            seatName = seatName,
            startTime = start.toString(),
            endTime = end.toString(),
            status = "confirmed",
            totalPrice = totalPrice
        )
    }

    fun findByUser(userId: Int): List<Booking> = transaction {
        (BookingsTable innerJoin GamingSeatsTable)
            .selectAll()
            .where { BookingsTable.userId eq userId }
            .orderBy(BookingsTable.createdAt, SortOrder.DESC)
            .map { it.toBooking() }
    }

    fun findAll(): List<Booking> = transaction {
        (BookingsTable innerJoin GamingSeatsTable)
            .selectAll()
            .orderBy(BookingsTable.createdAt, SortOrder.DESC)
            .map { it.toBooking() }
    }

    fun cancel(bookingId: Int, userId: Int, role: String): Boolean = transaction {
        val condition = if (role == "admin") {
            BookingsTable.id eq bookingId
        } else {
            (BookingsTable.id eq bookingId) and (BookingsTable.userId eq userId)
        }
        BookingsTable.update({ condition }) {
            it[status] = "cancelled"
        } > 0
    }

    private fun ResultRow.toBooking() = Booking(
        id = this[BookingsTable.id],
        userId = this[BookingsTable.userId],
        seatId = this[BookingsTable.seatId],
        seatName = this[GamingSeatsTable.name],
        startTime = this[BookingsTable.startTime].toString(),
        endTime = this[BookingsTable.endTime].toString(),
        status = this[BookingsTable.status],
        totalPrice = this[BookingsTable.totalPrice].toDouble()
    )
}
