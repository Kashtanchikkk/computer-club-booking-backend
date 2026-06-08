package com.example.data.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
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
        seedMapObjects(clubId, seedData.seatMapObjects)
    }

    fun seedMapObjectsForClub(clubId: Long) {
        val seedData = loadSeedData()
        seedMapObjects(clubId.toInt(), seedData.seatMapObjects)
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

    private fun seedMapObjects(clubId: Int, layouts: List<SeatMapObjectSeed>) {
        seedStaticMapObjects(clubId)
        seedSeatMapObjects(clubId, layouts)
    }

    private fun seedStaticMapObjects(clubId: Int) {
        val clubIdValue = clubId.toLong()
        val existingObjects = MapObjectsTable
            .selectAll()
            .where {
                (MapObjectsTable.clubId eq clubIdValue) and
                    ((MapObjectsTable.type eq MapObjectSeedType.ROOM.name) or (MapObjectsTable.type eq MapObjectSeedType.WALL.name))
            }
            .toList()

        val missingObjects = STATIC_MAP_OBJECTS.filterNot { seedObject ->
            existingObjects.any { it.matches(seedObject, clubIdValue) }
        }

        if (missingObjects.isNotEmpty()) {
            MapObjectsTable.batchInsert(missingObjects) { mapObject ->
                this[MapObjectsTable.clubId] = clubIdValue
                this[MapObjectsTable.type] = mapObject.type.name
                this[MapObjectsTable.title] = mapObject.title
                this[MapObjectsTable.x] = mapObject.x
                this[MapObjectsTable.y] = mapObject.y
                this[MapObjectsTable.width] = mapObject.width
                this[MapObjectsTable.height] = mapObject.height
                this[MapObjectsTable.seatId] = null
            }
        }
    }

    private fun seedSeatMapObjects(clubId: Int, layouts: List<SeatMapObjectSeed>) {
        val seatIdsByName = GamingSeatsTable
            .selectAll()
            .associate { it[GamingSeatsTable.name] to it[GamingSeatsTable.id] }
        val existingSeatObjectsBySeatId = MapObjectsTable
            .selectAll()
            .where { (MapObjectsTable.clubId eq clubId.toLong()) and (MapObjectsTable.type eq MapObjectSeedType.SEAT.name) }
            .associateBy { it[MapObjectsTable.seatId] }
        val existingSeatObjectsBySeedKey = MapObjectsTable
            .selectAll()
            .where { (MapObjectsTable.clubId eq clubId.toLong()) and (MapObjectsTable.type eq MapObjectSeedType.SEAT.name) }
            .associateBy { it.seedKey() }

        layouts.forEach { layout ->
            val seatId = seatIdsByName.getValue(layout.seatName)
            val existingObject = existingSeatObjectsBySeatId[seatId.toLong()]
                ?: existingSeatObjectsBySeedKey[layout.seedKey()]

            if (existingObject == null) {
                MapObjectsTable.insert {
                    it[MapObjectsTable.clubId] = clubId.toLong()
                    it[MapObjectsTable.type] = MapObjectSeedType.SEAT.name
                    it[MapObjectsTable.title] = layout.displayText
                    it[MapObjectsTable.x] = layout.x
                    it[MapObjectsTable.y] = layout.y
                    it[MapObjectsTable.width] = layout.width
                    it[MapObjectsTable.height] = layout.height
                    it[MapObjectsTable.seatId] = seatId.toLong()
                }
            } else if (existingObject.differsFrom(layout, clubId, seatId)) {
                MapObjectsTable.update({ MapObjectsTable.id eq existingObject[MapObjectsTable.id] }) {
                    it[MapObjectsTable.clubId] = clubId.toLong()
                    it[MapObjectsTable.type] = MapObjectSeedType.SEAT.name
                    it[MapObjectsTable.title] = layout.displayText
                    it[MapObjectsTable.x] = layout.x
                    it[MapObjectsTable.y] = layout.y
                    it[MapObjectsTable.width] = layout.width
                    it[MapObjectsTable.height] = layout.height
                    it[MapObjectsTable.seatId] = seatId.toLong()
                }
            }
        }
    }

    private fun ResultRow.matches(mapObject: StaticMapObjectSeed, clubId: Long): Boolean =
        this[MapObjectsTable.clubId] == clubId &&
            this[MapObjectsTable.type] == mapObject.type.name &&
            this[MapObjectsTable.title] == mapObject.title &&
            this[MapObjectsTable.x] == mapObject.x &&
            this[MapObjectsTable.y] == mapObject.y &&
            this[MapObjectsTable.width] == mapObject.width &&
            this[MapObjectsTable.height] == mapObject.height

    private fun ResultRow.differsFrom(layout: SeatMapObjectSeed, clubId: Int, seatId: Int): Boolean =
        this[MapObjectsTable.clubId] != clubId.toLong() ||
            this[MapObjectsTable.type] != MapObjectSeedType.SEAT.name ||
            this[MapObjectsTable.title] != layout.displayText ||
            this[MapObjectsTable.x] != layout.x ||
            this[MapObjectsTable.y] != layout.y ||
            this[MapObjectsTable.width] != layout.width ||
            this[MapObjectsTable.height] != layout.height ||
            this[MapObjectsTable.seatId] != seatId.toLong()

    private fun ResultRow.seedKey(): String =
        listOf(
            this[MapObjectsTable.title].orEmpty(),
            this[MapObjectsTable.x],
            this[MapObjectsTable.y],
            this[MapObjectsTable.width],
            this[MapObjectsTable.height]
        ).joinToString(separator = "|")

    private fun SeatMapObjectSeed.seedKey(): String =
        listOf(displayText, x, y, width, height).joinToString(separator = "|")

    private fun ResultRow.differsFrom(type: SeatTypeSeed): Boolean =
        this[SeatTypesTable.name] != type.name ||
            this[SeatTypesTable.pricePerHour].toDouble() != type.pricePerHour ||
            this[SeatTypesTable.processor] != type.processor ||
            this[SeatTypesTable.gpu] != type.gpu ||
            this[SeatTypesTable.ram] != type.ram ||
            this[SeatTypesTable.monitor] != type.monitor

    private const val SEED_DATA_FILE = "seed-data.json"

    private val STATIC_MAP_OBJECTS = listOf(
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "-> ВХОД", 8, 8, 220, 220),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "СТОЙКА\nАДМИНИСТРАТОРА", 228, 8, 210, 150),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "DUO 1", 438, 8, 170, 150),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "DUO 2", 608, 8, 165, 150),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "TRIO", 773, 8, 265, 150),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "SQUAD", 1038, 8, 105, 220),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "BOOTCAMP 1", 1143, 8, 165, 320),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "SQUAD", 122, 238, 110, 222),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "PS5 ROOM 1", 232, 238, 165, 210),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "STANDARD 1", 397, 218, 350, 165),
        StaticMapObjectSeed(MapObjectSeedType.WALL, null, 440, 328, 280, 6),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "STANDARD 2", 397, 388, 350, 170),
        StaticMapObjectSeed(MapObjectSeedType.WALL, null, 440, 498, 280, 6),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "BOOTCAMP 2", 887, 268, 130, 330),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "VIP ROOM", 1017, 335, 115, 190),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "PS5 ROOM 2", 1132, 335, 176, 190),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "WC", 1017, 525, 291, 82),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "DUO 3", 8, 525, 165, 120),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "DUO 4", 173, 525, 165, 120),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "STANDARD 3", 887, 625, 260, 270),
        StaticMapObjectSeed(MapObjectSeedType.ROOM, "TRIO", 1147, 625, 220, 155)
    )
}

private enum class MapObjectSeedType {
    ROOM,
    WALL,
    SEAT
}

private data class StaticMapObjectSeed(
    val type: MapObjectSeedType,
    val title: String?,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

@Serializable
private data class SeedData(
    val club: ClubSeed,
    val seatTypes: List<SeatTypeSeed>,
    val seatMapObjects: List<SeatMapObjectSeed>
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
private data class SeatMapObjectSeed(
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
