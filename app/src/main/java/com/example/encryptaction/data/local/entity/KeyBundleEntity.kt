package com.example.encryptaction.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "key_bundles",
    indices = [Index(value = ["userId"], unique = true)]
)
data class KeyBundleEntity(
    @PrimaryKey val userId: String,
    /** Base64-encoded DER bytes of the RSA-4096 encryption public key */
    val encryptionPublicKeyBase64: String,
    /** Base64-encoded DER bytes of the EC P-256 signing public key */
    val signingPublicKeyBase64: String,
    val createdAt: Long,
    /** Hex-encoded SHA-256 fingerprint of the encryption public key */
    val fingerprint: String
)
