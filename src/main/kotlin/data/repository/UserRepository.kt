package com.example.data.repository

import com.example.data.db.UsersTable
import com.example.domain.model.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class UserRepository {

    fun findByEmail(email: String): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun findById(id: Int): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun findAuthDataByEmail(email: String): UserAuthData? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .map { UserAuthData(it.toUser(), it[UsersTable.passwordHash]) }
            .singleOrNull()
    }

    fun create(email: String, passwordHash: String, name: String): User = transaction {
        val id = UsersTable.insert {
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.name] = name
            it[role] = "user"
            it[createdAt] = LocalDateTime.now()
        }[UsersTable.id]

        User(
            id = id,
            email = email,
            name = name,
            role = "user"
        )
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        name = this[UsersTable.name],
        role = this[UsersTable.role]
    )
}

data class UserAuthData(
    val user: User,
    val passwordHash: String
)
