package com.example.encryptaction.domain.usecase.key

import com.example.encryptaction.core.crypto.CryptoError
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.core.crypto.KeyStoreManager
import com.example.encryptaction.core.crypto.getOrNull
import com.example.encryptaction.domain.model.UserKeyBundle
import com.example.encryptaction.domain.repository.UserRepository
import java.security.MessageDigest
import java.time.Instant
import javax.inject.Inject

/**
 * Generates key pairs in Android Keystore and stores the public key bundle
 * in the local user directory so other team members can use it.
 *
 * Should be called once when a user account is first created on device.
 */
class GenerateUserKeysUseCase @Inject constructor(
    private val keyStoreManager: KeyStoreManager,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): CryptoResult<UserKeyBundle> {
        // Generate both key pairs in Keystore
        val genResult = keyStoreManager.generateKeyPairs(userId)
        if (genResult is CryptoResult.Failure) return genResult

        // Export public keys for sharing
        val encPubBytes = keyStoreManager.exportEncryptionPublicKeyBytes(userId).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyGenerationFailed("Failed to export encryption public key"))

        val signPubBytes = keyStoreManager.exportSigningPublicKeyBytes(userId).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyGenerationFailed("Failed to export signing public key"))

        // Compute fingerprint: hex SHA-256 of the encryption public key
        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(encPubBytes)
            .joinToString("") { "%02x".format(it) }
            .chunked(4).joinToString(":")  // e.g. "ab12:cd34:..."

        val bundle = UserKeyBundle(
            userId = userId,
            encryptionPublicKeyBytes = encPubBytes,
            signingPublicKeyBytes = signPubBytes,
            createdAt = Instant.now(),
            fingerprint = fingerprint
        )

        userRepository.saveKeyBundle(bundle)
        return CryptoResult.Success(bundle)
    }
}
