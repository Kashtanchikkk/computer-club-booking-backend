package com.example.data.db

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

object GamingSeatsTable : Table("gaming_seats") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val type = varchar("type", 50)
    val pricePerHour = decimal("price_per_hour", 10, 2)
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)
}

object BookingsTable : Table("bookings") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(UsersTable.id)
    val seatId = integer("seat_id").references(GamingSeatsTable.id)
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")
    val status = varchar("status", 20).default("pending")
    val totalPrice = decimal("total_price", 10, 2)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}