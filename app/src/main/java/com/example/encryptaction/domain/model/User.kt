package com.example.encryptaction.domain.model

import java.time.Instant

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val role: UserRole,
    val keyBundle: UserKeyBundle?,
    val createdAt: Instant,
    val isActive: Boolean = true
)

enum class UserRole {
    ADMIN,
    MEMBER;

    val displayName: String get() = when (this) {
        ADMIN -> "Administrator"
        MEMBER -> "Member"
    }
}

/**
 * Holds the DER-encoded public keys for a user — safe to share with team members.
 * Private keys never leave the Android Keystore.
 */
data class UserKeyBundle(
    val userId: String,
    /** RSA-4096 public key (DER/X.509) for encryption key wrapping */
    val encryptionPublicKeyBytes: ByteArray,
    /** EC P-256 public key (DER/X.509) for signature verification */
    val signingPublicKeyBytes: ByteArray,
    val createdAt: Instant,
    /** Fingerprint (SHA-256 of encryptionPublicKey) for easy identification */
    val fingerprint: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UserKeyBundle
        return userId == other.userId &&
                encryptionPublicKeyBytes.contentEquals(other.encryptionPublicKeyBytes) &&
                signingPublicKeyBytes.contentEquals(other.signingPublicKeyBytes)
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + encryptionPublicKeyBytes.contentHashCode()
        result = 31 * result + signingPublicKeyBytes.contentHashCode()
        return result
    }
}
