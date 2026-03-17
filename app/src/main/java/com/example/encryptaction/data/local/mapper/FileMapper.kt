package com.example.encryptaction.data.local.mapper

import com.example.encryptaction.data.local.entity.EncryptedFileEntity
import com.example.encryptaction.data.local.entity.SignedFileEntity
import com.example.encryptaction.domain.model.EncryptedFile
import com.example.encryptaction.domain.model.EncryptedFileStatus
import com.example.encryptaction.domain.model.SignedFile
import java.time.Instant

fun EncryptedFileEntity.toDomain() = EncryptedFile(
    id = id,
    originalName = originalName,
    mimeType = mimeType,
    originalSizeBytes = originalSizeBytes,
    encryptedFilePath = encryptedFilePath,
    senderId = senderId ?: "",
    recipientId = recipientId ?: "",
    encryptedAt = Instant.ofEpochMilli(encryptedAt),
    status = EncryptedFileStatus.valueOf(status),
    plaintextHash = plaintextHash
)

fun EncryptedFile.toEntity() = EncryptedFileEntity(
    id = id,
    originalName = originalName,
    mimeType = mimeType,
    originalSizeBytes = originalSizeBytes,
    encryptedFilePath = encryptedFilePath,
    senderId = senderId.ifEmpty { null },
    recipientId = recipientId.ifEmpty { null },
    encryptedAt = encryptedAt.toEpochMilli(),
    status = status.name,
    plaintextHash = plaintextHash
)

fun SignedFileEntity.toDomain() = SignedFile(
    id = id,
    originalName = originalName,
    filePath = filePath,
    signerId = signerId ?: "",
    signatureFilePath = signatureFilePath,
    signedAt = Instant.ofEpochMilli(signedAt),
    isVerified = isVerified,
    fileHash = fileHash
)

fun SignedFile.toEntity() = SignedFileEntity(
    id = id,
    originalName = originalName,
    filePath = filePath,
    signerId = signerId.ifEmpty { null },
    signatureFilePath = signatureFilePath,
    signedAt = signedAt.toEpochMilli(),
    isVerified = isVerified,
    fileHash = fileHash
)
