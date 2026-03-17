package com.example.encryptaction.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "encrypted_files",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipientId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["senderId"]),
        Index(value = ["recipientId"])
    ]
)
data class EncryptedFileEntity(
    @PrimaryKey val id: String,
    val originalName: String,
    val mimeType: String,
    val originalSizeBytes: Long,
    val encryptedFilePath: String,
    val senderId: String?,
    val recipientId: String?,
    val encryptedAt: Long,
    val status: String,
    val plaintextHash: String
)
