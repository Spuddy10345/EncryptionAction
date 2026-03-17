package com.example.encryptaction.domain.model

import java.time.Instant

data class EncryptedFile(
    val id: String,
    /** Original filename before encryption */
    val originalName: String,
    /** MIME type of the original file */
    val mimeType: String,
    /** File size of the original plaintext in bytes */
    val originalSizeBytes: Long,
    /** Path to the .eaep package on device storage */
    val encryptedFilePath: String,
    val senderId: String,
    val recipientId: String,
    val encryptedAt: Instant,
    val status: EncryptedFileStatus,
    /** SHA-256 hex of the original plaintext — for integrity confirmation */
    val plaintextHash: String
)

enum class EncryptedFileStatus {
    PENDING_SEND,
    SENT,
    RECEIVED,
    DECRYPTED,
    FAILED
}

data class SignedFile(
    val id: String,
    val originalName: String,
    val filePath: String,
    val signerId: String,
    val signatureFilePath: String,
    val signedAt: Instant,
    val isVerified: Boolean?,
    /** SHA-256 hex of the signed file */
    val fileHash: String
)
