package com.example.encryptaction.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "signed_files",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["signerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["signerId"])]
)
data class SignedFileEntity(
    @PrimaryKey val id: String,
    val originalName: String,
    val filePath: String,
    val signerId: String?,
    val signatureFilePath: String,
    val signedAt: Long,
    val isVerified: Boolean?,
    val fileHash: String
)
