package com.example.data.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object DatabaseSeeder {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun seedInitialData() {
        val seedData = loadSeedData()
        val clubId = seedClub(seedData.club)
        val activeTypeIds = seedSeats(clubId, seedData.seatTypes)
        deactivateSeatsForRemovedTypes(clubId, activeTypeIds)
        seedSeatLayouts(seedData.seatLayouts)
    }

    private fun loadSeedData(): SeedData {
        val seedJson = requireNotNull(DatabaseSeeder::class.java.classLoader.getResource(SEED_DATA_FILE)) {
            "Seed data file $SEED_DATA_FILE not found"
        }.readText()

        return json.decodeFromString(seedJson)
    }

    private fun seedClub(club: ClubSeed): Int {
        val existingClub = ComputerClubsTable
            .selectAll()
            .where { ComputerClubsTable.name eq club.name }
            .singleOrNull()
            ?: ComputerClubsTable
                .selectAll()
                .singleOrNull()

        if (existingClub != null) return existingClub[ComputerClubsTable.id]

        return ComputerClubsTable.insert {
            it[name] = club.name
            it[address] = club.address
            it[rating] = club.rating.toBigDecimal()
        }[ComputerClubsTable.id]
    }

    private fun seedSeats(clubId: Int, seatTypes: List<SeatTypeSeed>): Set<Int> {
        val seatTypeIds = seatTypes.associate { type ->
            val existingType = SeatTypesTable
                .selectAll()
                .where { SeatTypesTable.name eq type.name }
                .singleOrNull()

            val typeId = existingType?.get(SeatTypesTable.id)
                ?: SeatTypesTable.insert {
                    it[name] = type.name
                    it[pricePerHour] = type.pricePerHour.toBigDecimal()
                    it[processor] = type.processor
                    it[gpu] = type.gpu
                    it[ram] = type.ram
                    it[monitor] = type.monitor
                }[SeatTypesTable.id]

            if (existingType != null && existingType.differsFrom(type)) {
                updateSeatType(typeId, type)
            }

            type.name to typeId
        }

        seatTypes.forEach { type ->
            syncSeatsForType(clubId = clubId, type = type, typeId = seatTypeIds.getValue(type.name))
        }

        return seatTypeIds.values.toSet()
    }

    private fun updateSeatType(typeId: Int, type: SeatTypeSeed) {
        SeatTypesTable.update({ SeatTypesTable.id eq typeId }) {
            it[name] = type.name
            it[pricePerHour] = type.pricePerHour.toBigDecimal()
            it[processor] = type.processor
            it[gpu] = type.gpu
            it[ram] = type.ram
            it[monitor] = type.monitor
        }
    }

    private fun syncSeatsForType(clubId: Int, type: SeatTypeSeed, typeId: Int) {
        val existingSeats = GamingSeatsTable
            .selectAll()
            .where { (GamingSeatsTable.clubId eq clubId) and (GamingSeatsTable.typeId eq typeId) }
            .toList()
        val existingSeatNames = existingSeats.map { it[GamingSeatsTable.name] }.toSet()
        val missingSeats = type.seats.filterNot { it in existingSeatNames }

        existingSeats
            .filter { it[GamingSeatsTable.name] !in type.seats }
            .forEach { extraSeat ->
                GamingSeatsTable.update({ GamingSeatsTable.id eq extraSeat[GamingSeatsTable.id] }) {
                    it[isActive] = false
                }
            }

        existingSeats
            .filter { it[GamingSeatsTable.name] in type.seats && !it[GamingSeatsTable.isActive] }
            .forEach { inactiveSeat ->
                GamingSeatsTable.update({ GamingSeatsTable.id eq inactiveSeat[GamingSeatsTable.id] }) {
                    it[isActive] = true
                }
            }

        if (missingSeats.isNotEmpty()) {
            GamingSeatsTable.batchInsert(missingSeats) { seatName ->
                this[GamingSeatsTable.clubId] = clubId
                this[GamingSeatsTable.typeId] = typeId
                this[GamingSeatsTable.name] = seatName
                this[GamingSeatsTable.isActive] = true
            }
        }
    }

    private fun deactivateSeatsForRemovedTypes(clubId: Int, activeTypeIds: Set<Int>) {
        GamingSeatsTable
            .selectAll()
            .where { GamingSeatsTable.clubId eq clubId }
            .filter { it[GamingSeatsTable.typeId] !in activeTypeIds && it[GamingSeatsTable.isActive] }
            .forEach { staleSeat ->
                GamingSeatsTable.update({ GamingSeatsTable.id eq staleSeat[GamingSeatsTable.id] }) {
                    it[isActive] = false
                }
            }
    }

    private fun seedSeatLayouts(layouts: List<SeatLayoutSeed>) {
        val seatIdsByName = GamingSeatsTable
            .selectAll()
            .associate { it[GamingSeatsTable.name] to it[GamingSeatsTable.id] }
        val existingLayouts = SeatLayoutsTable
            .selectAll()
            .associateBy { it[SeatLayoutsTable.id] }
        val missingLayouts = layouts.filterNot { it.id in existingLayouts }

        layouts.forEach { layout ->
            val seatId = seatIdsByName.getValue(layout.seatName)
            val existingLayout = existingLayouts[layout.id]

            if (existingLayout != null && existingLayout.differsFrom(layout, seatId)) {
                SeatLayoutsTable.update({ SeatLayoutsTable.id eq layout.id }) {
                    it[SeatLayoutsTable.seatId] = seatId
                    it[label] = layout.label
                    it[room] = layout.room
                    it[x] = layout.x
                    it[y] = layout.y
                    it[width] = layout.width
                    it[height] = layout.height
                    it[color] = layout.color
                    it[displayText] = layout.displayText
                }
            }
        }

        if (missingLayouts.isNotEmpty()) {
            SeatLayoutsTable.batchInsert(missingLayouts) { layout ->
                this[SeatLayoutsTable.id] = layout.id
                this[SeatLayoutsTable.seatId] = seatIdsByName.getValue(layout.seatName)
                this[SeatLayoutsTable.label] = layout.label
                this[SeatLayoutsTable.room] = layout.room
                this[SeatLayoutsTable.x] = layout.x
                this[SeatLayoutsTable.y] = layout.y
                this[SeatLayoutsTable.width] = layout.width
                this[SeatLayoutsTable.height] = layout.height
                this[SeatLayoutsTable.color] = layout.color
                this[SeatLayoutsTable.displayText] = layout.displayText
            }
        }
    }

    private fun ResultRow.differsFrom(layout: SeatLayoutSeed, seatId: Int): Boolean =
        this[SeatLayoutsTable.seatId] != seatId ||
            this[SeatLayoutsTable.label] != layout.label ||
            this[SeatLayoutsTable.room] != layout.room ||
            this[SeatLayoutsTable.x] != layout.x ||
            this[SeatLayoutsTable.y] != layout.y ||
            this[SeatLayoutsTable.width] != layout.width ||
            this[SeatLayoutsTable.height] != layout.height ||
            this[SeatLayoutsTable.color] != layout.color ||
            this[SeatLayoutsTable.displayText] != layout.displayText

    private fun ResultRow.differsFrom(type: SeatTypeSeed): Boolean =
        this[SeatTypesTable.name] != type.name ||
            this[SeatTypesTable.pricePerHour].toDouble() != type.pricePerHour ||
            this[SeatTypesTable.processor] != type.processor ||
            this[SeatTypesTable.gpu] != type.gpu ||
            this[SeatTypesTable.ram] != type.ram ||
            this[SeatTypesTable.monitor] != type.monitor

    private const val SEED_DATA_FILE = "seed-data.json"
}

@Serializable
private data class SeedData(
    val club: ClubSeed,
    val seatTypes: List<SeatTypeSeed>,
    val seatLayouts: List<SeatLayoutSeed>
)

@Serializable
private data class ClubSeed(
    val name: String,
    val address: String,
    val rating: Double
)

@Serializable
private data class SeatTypeSeed(
    val name: String,
    val pricePerHour: Double,
    val processor: String,
    val gpu: String,
    val ram: String,
    val monitor: String,
    val seats: List<String>
)

@Serializable
private data class SeatLayoutSeed(
    val id: String,
    val label: String,
    val room: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val color: String,
    val displayText: String,
    val seatName: String = id
)
