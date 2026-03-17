package com.example.encryptaction.domain.usecase.key

import android.util.Base64
import com.example.encryptaction.core.crypto.CryptoError
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Exports both public keys for a user as a PEM-like text bundle
 * that can be shared (e.g., copy/paste, QR code, file export).
 *
 * Format:
 *   -----BEGIN EA ENCRYPTION PUBLIC KEY-----
 *   <base64 of DER>
 *   -----END EA ENCRYPTION PUBLIC KEY-----
 *   -----BEGIN EA SIGNING PUBLIC KEY-----
 *   <base64 of DER>
 *   -----END EA SIGNING PUBLIC KEY-----
 */
class ExportPublicKeyUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): CryptoResult<String> {
        val bundle = userRepository.getKeyBundle(userId)
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("No key bundle found for user $userId"))

        val encPem = buildPemBlock(
            "EA ENCRYPTION PUBLIC KEY",
            bundle.encryptionPublicKeyBytes
        )
        val signPem = buildPemBlock(
            "EA SIGNING PUBLIC KEY",
            bundle.signingPublicKeyBytes
        )
        val metadata = "# EncryptAction Key Bundle\n# UserID: $userId\n# Fingerprint: ${bundle.fingerprint}\n\n"
        return CryptoResult.Success(metadata + encPem + "\n" + signPem)
    }

    private fun buildPemBlock(label: String, keyBytes: ByteArray): String {
        val b64 = Base64.encodeToString(keyBytes, Base64.NO_WRAP)
        val wrapped = b64.chunked(64).joinToString("\n")
        return "-----BEGIN $label-----\n$wrapped\n-----END $label-----\n"
    }
}
