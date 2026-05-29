package com.example.data.db

import com.example.domain.model.BookingStatus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 100)
    val role = varchar("role", 20).default("user")
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ComputerClubsTable : Table("computer_clubs") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val address = varchar("address", 255)
    val rating = decimal("rating", 3, 1).default(5.0.toBigDecimal())

    override val primaryKey = PrimaryKey(id)
}

object SeatTypesTable : Table("seat_types") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val pricePerHour = decimal("price_per_hour", 10, 2)
    val processor = varchar("processor", 100)
    val gpu = varchar("gpu", 100)
    val ram = varchar("ram", 50)
    val monitor = varchar("monitor", 50)

    override val primaryKey = PrimaryKey(id)
}

object GamingSeatsTable : Table("gaming_seats") {
    val id = integer("id").autoIncrement()
    val clubId = integer("club_id").default(1)
    val typeId = integer("type_id").default(1)
    val name = varchar("name", 50)
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)
}

object SeatLayoutsTable : Table("seat_layouts") {
    val id = varchar("id", 50)
    val seatId = optReference("seat_id", GamingSeatsTable.id).uniqueIndex()
    val label = varchar("label", 100)
    val room = varchar("room", 100)
    val x = integer("x")
    val y = integer("y")
    val width = integer("width")
    val height = integer("height")
    val color = varchar("color", 20)
    val displayText = varchar("display_text", 50)

    override val primaryKey = PrimaryKey(id)
}

object BookingsTable : Table("bookings") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(UsersTable.id)
    val seatId = integer("seat_id").references(GamingSeatsTable.id)
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")
    val status = varchar("status", 20).default(BookingStatus.Pending.value)
    val totalPrice = decimal("total_price", 10, 2)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
