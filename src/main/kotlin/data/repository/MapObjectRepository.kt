package com.example.data.repository

import com.example.data.db.DatabaseSeeder
import com.example.data.db.MapObjectsTable
import com.example.domain.model.MapObject
import com.example.domain.model.MapObjectType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class MapObjectRepository {
    fun findByClubId(clubId: Long): List<MapObject> {
        val mapObjects = selectByClubId(clubId)
        if (mapObjects.isNotEmpty()) return mapObjects

        transaction {
            DatabaseSeeder.seedMapObjectsForClub(clubId)
        }

        return selectByClubId(clubId)
    }

    private fun selectByClubId(clubId: Long): List<MapObject> = transaction {
        MapObjectsTable
            .selectAll()
            .where { MapObjectsTable.clubId eq clubId }
            .orderBy(MapObjectsTable.id to SortOrder.ASC)
            .map { it.toMapObject() }
    }

    private fun ResultRow.toMapObject() = MapObject(
        id = this[MapObjectsTable.id],
        clubId = this[MapObjectsTable.clubId],
        type = MapObjectType.valueOf(this[MapObjectsTable.type]),
        title = this[MapObjectsTable.title],
        x = this[MapObjectsTable.x],
        y = this[MapObjectsTable.y],
        width = this[MapObjectsTable.width],
        height = this[MapObjectsTable.height],
        seatId = this[MapObjectsTable.seatId]
    )
}
