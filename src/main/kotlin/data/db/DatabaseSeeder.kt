package com.example.data.db

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object DatabaseSeeder {
    fun seedInitialData() {
        seedClub()
        seedSeats()
        seedSeatLayouts()
    }

    private fun seedClub() {
        if (ComputerClubsTable.selectAll().count() > 0L) return

        ComputerClubsTable.insert {
            it[name] = "Cyber Arena"
            it[address] = "Каширский проезд, 25к4"
            it[rating] = 5.0.toBigDecimal()
        }
    }

    private fun seedSeats() {
        val standardSeats = (1..24).map { "%02d".format(it) } + (30..37).map { "%02d".format(it) }
        val vipSeats = listOf(
            "D1-1", "D1-2", "D2-1", "D2-2", "D3-1", "D3-2", "D4-1", "D4-2",
            "T1-1", "T1-2", "T1-3", "T2-1", "T2-2", "T2-3",
            "S1-1", "S1-2", "S1-3", "S1-4",
            "SQ-25", "SQ-24", "SQ-23", "SQ-22",
            "V1", "V2", "V3"
        )

        val seatTypes = listOf(
            SeatSeed("Standard", 180.0, "Intel Core i5-13600KF", "GeForce RTX 4060 Ti", "16 ГБ DDR4", "24\" 144 Гц", standardSeats),
            SeatSeed("VIP", 320.0, "Intel Core i7-14700K", "GeForce RTX 4080", "32 ГБ DDR5", "27\" 240 Гц", vipSeats),
            SeatSeed("Bootcamp", 420.0, "AMD Ryzen 7 7800X3D", "GeForce RTX 4090", "64 ГБ DDR5", "32\" 240 Гц", listOf("BC1-1", "BC1-2", "BC1-3", "BC1-4", "BC1-5", "BC2-1", "BC2-2", "BC2-3", "BC2-4", "BC2-5")),
            SeatSeed("PS Zone", 260.0, "PlayStation 5", "DualSense Station", "4 геймпада", "55\" 120 Гц", listOf("PS5-1", "PS5-2"))
        )

        val seatTypeIds = seatTypes.associate { type ->
            val existingType = SeatTypesTable
                .selectAll()
                .where { SeatTypesTable.name eq type.type }
                .singleOrNull()

            val typeId = existingType?.get(SeatTypesTable.id)
                ?: SeatTypesTable.insert {
                    it[name] = type.type
                    it[pricePerHour] = type.pricePerHour.toBigDecimal()
                    it[processor] = type.processor
                    it[gpu] = type.gpu
                    it[ram] = type.ram
                    it[monitor] = type.monitor
                }[SeatTypesTable.id]

            type.type to typeId
        }

        seatTypes.forEach { type ->
            syncSeatsForType(clubId = 1, type = type, typeId = seatTypeIds.getValue(type.type))
        }
        deactivateSeatsForRemovedTypes(clubId = 1, activeTypeIds = seatTypeIds.values.toSet())
    }

    private fun syncSeatsForType(clubId: Int, type: SeatSeed, typeId: Int) {
        val existingSeats = GamingSeatsTable
            .selectAll()
            .where { (GamingSeatsTable.clubId eq clubId) and (GamingSeatsTable.typeId eq typeId) }
            .toList()
        val existingSeatNames = existingSeats.map { it[GamingSeatsTable.name] }.toSet()
        val missingSeats = type.seatNames.filterNot { it in existingSeatNames }

        existingSeats
            .filter { it[GamingSeatsTable.name] !in type.seatNames }
            .forEach { extraSeat ->
                GamingSeatsTable.update({ GamingSeatsTable.id eq extraSeat[GamingSeatsTable.id] }) {
                    it[GamingSeatsTable.isActive] = false
                }
            }

        existingSeats
            .filter { it[GamingSeatsTable.name] in type.seatNames && !it[GamingSeatsTable.isActive] }
            .forEach { inactiveSeat ->
                GamingSeatsTable.update({ GamingSeatsTable.id eq inactiveSeat[GamingSeatsTable.id] }) {
                    it[GamingSeatsTable.isActive] = true
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
                    it[GamingSeatsTable.isActive] = false
                }
            }
    }

    private fun seedSeatLayouts() {
        val existingLayoutIds = SeatLayoutsTable
            .selectAll()
            .map { it[SeatLayoutsTable.id] }
            .toSet()
        val missingLayouts = seatLayoutSeeds().filterNot { it.id in existingLayoutIds }

        // Сидинг схемы переносит бывший клиентский хардкод в БД и не создает дубликаты.
        if (missingLayouts.isNotEmpty()) {
            SeatLayoutsTable.batchInsert(missingLayouts) { layout ->
                this[SeatLayoutsTable.id] = layout.id
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

    private fun seatLayoutSeeds(): List<SeatLayoutSeed> = listOf(
        SeatLayoutSeed("D1-1", "Duo 1 · D1-1", "комната для двух игроков", 470, 70, color = "#BDEBFF"),
        SeatLayoutSeed("D1-2", "Duo 1 · D1-2", "комната для двух игроков", 535, 70, color = "#BDEBFF"),
        SeatLayoutSeed("D2-1", "Duo 2 · D2-1", "комната для двух игроков", 650, 70, color = "#BDEBFF"),
        SeatLayoutSeed("D2-2", "Duo 2 · D2-2", "комната для двух игроков", 720, 70, color = "#BDEBFF"),
        SeatLayoutSeed("D3-1", "Duo 3 · D3-1", "комната для двух игроков", 35, 592, color = "#BDEBFF"),
        SeatLayoutSeed("D3-2", "Duo 3 · D3-2", "комната для двух игроков", 95, 592, color = "#BDEBFF"),
        SeatLayoutSeed("D4-1", "Duo 4 · D4-1", "комната для двух игроков", 205, 592, color = "#BDEBFF"),
        SeatLayoutSeed("D4-2", "Duo 4 · D4-2", "комната для двух игроков", 270, 592, color = "#BDEBFF"),
        SeatLayoutSeed("T1-1", "Trio · T1-1", "комната для трех игроков", 820, 72, color = "#FFD7FF"),
        SeatLayoutSeed("T1-2", "Trio · T1-2", "комната для трех игроков", 885, 72, color = "#FFD7FF"),
        SeatLayoutSeed("T1-3", "Trio · T1-3", "комната для трех игроков", 950, 72, color = "#FFD7FF"),
        SeatLayoutSeed("T2-1", "Trio · T2-1", "комната для трех игроков", 1175, 685, color = "#FFD7FF"),
        SeatLayoutSeed("T2-2", "Trio · T2-2", "комната для трех игроков", 1240, 685, color = "#FFD7FF"),
        SeatLayoutSeed("T2-3", "Trio · T2-3", "комната для трех игроков", 1305, 685, color = "#FFD7FF"),
        SeatLayoutSeed("S1-1", "Squad · S1-1", "комната для команды", 1075, 28, color = "#FFE8A8"),
        SeatLayoutSeed("S1-2", "Squad · S1-2", "комната для команды", 1075, 72, color = "#FFE8A8"),
        SeatLayoutSeed("S1-3", "Squad · S1-3", "комната для команды", 1075, 116, color = "#FFE8A8"),
        SeatLayoutSeed("S1-4", "Squad · S1-4", "комната для команды", 1075, 160, color = "#FFE8A8"),
        SeatLayoutSeed("V1", "VIP Room · V1", "VIP комната", 1055, 372, color = "#BDEBFF"),
        SeatLayoutSeed("V2", "VIP Room · V2", "VIP комната", 1055, 422, color = "#BDEBFF"),
        SeatLayoutSeed("V3", "VIP Room · V3", "VIP комната", 1055, 472, color = "#BDEBFF"),
        SeatLayoutSeed("BC1-1", "Bootcamp 1 · BC1-1", "буткемп 1", 1200, 68, color = "#E1FFD8"),
        SeatLayoutSeed("BC1-2", "Bootcamp 1 · BC1-2", "буткемп 1", 1200, 116, color = "#E1FFD8"),
        SeatLayoutSeed("BC1-3", "Bootcamp 1 · BC1-3", "буткемп 1", 1200, 164, color = "#E1FFD8"),
        SeatLayoutSeed("BC1-4", "Bootcamp 1 · BC1-4", "буткемп 1", 1200, 212, color = "#E1FFD8"),
        SeatLayoutSeed("BC1-5", "Bootcamp 1 · BC1-5", "буткемп 1", 1200, 260, color = "#E1FFD8"),
        SeatLayoutSeed("BC2-1", "Bootcamp 2 · BC2-1", "буткемп 2", 915, 322, color = "#E1FFD8"),
        SeatLayoutSeed("BC2-2", "Bootcamp 2 · BC2-2", "буткемп 2", 915, 372, color = "#E1FFD8"),
        SeatLayoutSeed("BC2-3", "Bootcamp 2 · BC2-3", "буткемп 2", 915, 422, color = "#E1FFD8"),
        SeatLayoutSeed("BC2-4", "Bootcamp 2 · BC2-4", "буткемп 2", 915, 472, color = "#E1FFD8"),
        SeatLayoutSeed("BC2-5", "Bootcamp 2 · BC2-5", "буткемп 2", 915, 522, color = "#E1FFD8"),
        SeatLayoutSeed("PS5-1", "PS5 Room 1 · PS5-1", "PlayStation зона", 285, 345, 62, 42, "#CFF4FF"),
        SeatLayoutSeed("PS5-2", "PS5 Room 2 · PS5-2", "PlayStation зона", 1195, 405, 62, 42, "#CFF4FF"),
        SeatLayoutSeed("SQ-25", "Squad · 25", "левая squad зона", 150, 265, color = "#FFF1B8", displayText = "25"),
        SeatLayoutSeed("SQ-24", "Squad · 24", "левая squad зона", 150, 312, color = "#FFF1B8", displayText = "24"),
        SeatLayoutSeed("SQ-23", "Squad · 23", "левая squad зона", 150, 359, color = "#FFF1B8", displayText = "23"),
        SeatLayoutSeed("SQ-22", "Squad · 22", "левая squad зона", 150, 406, color = "#FFF1B8", displayText = "22"),
        standardSeatLayout("01", 445, 288),
        standardSeatLayout("02", 490, 288),
        standardSeatLayout("03", 535, 288),
        standardSeatLayout("04", 580, 288),
        standardSeatLayout("05", 625, 288),
        standardSeatLayout("06", 670, 288),
        standardSeatLayout("07", 445, 338),
        standardSeatLayout("08", 490, 338),
        standardSeatLayout("09", 535, 338, "#FFE04A"),
        standardSeatLayout("10", 580, 338),
        standardSeatLayout("11", 625, 338),
        standardSeatLayout("12", 670, 338),
        standardSeatLayout("13", 445, 458),
        standardSeatLayout("14", 490, 458),
        standardSeatLayout("15", 535, 458),
        standardSeatLayout("16", 580, 458),
        standardSeatLayout("17", 625, 458),
        standardSeatLayout("18", 670, 458),
        standardSeatLayout("19", 445, 508),
        standardSeatLayout("20", 490, 508),
        standardSeatLayout("21", 535, 508),
        standardSeatLayout("22", 580, 508),
        standardSeatLayout("23", 625, 508),
        standardSeatLayout("24", 670, 508),
        standardSeatLayout("30", 920, 645),
        standardSeatLayout("31", 970, 645),
        standardSeatLayout("32", 1020, 645),
        standardSeatLayout("33", 1070, 645),
        standardSeatLayout("37", 920, 695),
        standardSeatLayout("36", 920, 740),
        standardSeatLayout("35", 920, 785),
        standardSeatLayout("34", 920, 830)
    )

    private fun standardSeatLayout(
        id: String,
        x: Int,
        y: Int,
        color: String = "#FFF1B8"
    ): SeatLayoutSeed {
        val number = id.toIntOrNull()?.toString() ?: id

        return SeatLayoutSeed(
            id = id,
            label = "Standard · PC-$number",
            room = "стандартная зона",
            x = x,
            y = y,
            width = 44,
            height = 38,
            color = color,
            displayText = "PC-$number"
        )
    }
}

private data class SeatSeed(
    val type: String,
    val pricePerHour: Double,
    val processor: String,
    val gpu: String,
    val ram: String,
    val monitor: String,
    val seatNames: List<String>
)

private data class SeatLayoutSeed(
    val id: String,
    val label: String,
    val room: String,
    val x: Int,
    val y: Int,
    val width: Int = 50,
    val height: Int = 42,
    val color: String,
    val displayText: String = id
)
