package com.example.encryptaction.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val passwordHash: String,
    val role: String,
    val createdAt: Long,
    val isActive: Boolean
)
