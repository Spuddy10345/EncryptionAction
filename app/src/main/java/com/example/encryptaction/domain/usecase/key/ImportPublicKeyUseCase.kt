package com.example.encryptaction.domain.usecase.key

import android.util.Base64
import com.example.encryptaction.core.crypto.CryptoError
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.core.crypto.RsaCryptoEngine
import com.example.encryptaction.core.crypto.SigningEngine
import com.example.encryptaction.core.crypto.getOrNull
import com.example.encryptaction.domain.model.UserKeyBundle
import com.example.encryptaction.domain.repository.UserRepository
import java.security.MessageDigest
import java.time.Instant
import javax.inject.Inject

/**
 * Parses and imports a PEM-format key bundle exported by [ExportPublicKeyUseCase].
 * Validates both keys can be reconstructed before saving to the directory.
 */
class ImportPublicKeyUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val rsaEngine: RsaCryptoEngine,
    private val signingEngine: SigningEngine
) {
    suspend operator fun invoke(userId: String, pemText: String): CryptoResult<UserKeyBundle> {
        val encKeyBytes = extractPemBlock(pemText, "EA ENCRYPTION PUBLIC KEY")
            ?: return CryptoResult.Failure(CryptoError.InvalidPackage("Missing encryption public key block"))

        val signKeyBytes = extractPemBlock(pemText, "EA SIGNING PUBLIC KEY")
            ?: return CryptoResult.Failure(CryptoError.InvalidPackage("Missing signing public key block"))

        // Validate keys can be decoded
        rsaEngine.publicKeyFromBytes(encKeyBytes).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Encryption public key is invalid"))

        signingEngine.publicKeyFromBytes(signKeyBytes).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Signing public key is invalid"))

        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(encKeyBytes)
            .joinToString("") { "%02x".format(it) }
            .chunked(4).joinToString(":")

        val bundle = UserKeyBundle(
            userId = userId,
            encryptionPublicKeyBytes = encKeyBytes,
            signingPublicKeyBytes = signKeyBytes,
            createdAt = Instant.now(),
            fingerprint = fingerprint
        )

        userRepository.saveKeyBundle(bundle)
        return CryptoResult.Success(bundle)
    }

    private fun extractPemBlock(pem: String, label: String): ByteArray? {
        val header = "-----BEGIN $label-----"
        val footer = "-----END $label-----"
        val start = pem.indexOf(header).takeIf { it >= 0 }?.plus(header.length) ?: return null
        val end = pem.indexOf(footer, start).takeIf { it >= 0 } ?: return null
        val b64 = pem.substring(start, end).trim().replace("\n", "").replace("\r", "")
        return try {
            Base64.decode(b64, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
