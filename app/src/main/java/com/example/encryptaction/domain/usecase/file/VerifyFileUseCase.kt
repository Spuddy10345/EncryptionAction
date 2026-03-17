package com.example.encryptaction.domain.usecase.file

import android.util.Base64
import com.example.encryptaction.core.crypto.CryptoError
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.core.crypto.HybridCryptoEngine
import com.example.encryptaction.core.crypto.SigningEngine
import com.example.encryptaction.core.crypto.getOrNull
import com.example.encryptaction.domain.model.SignedFile
import com.example.encryptaction.domain.repository.FileRepository
import com.example.encryptaction.domain.repository.UserRepository
import com.google.gson.Gson
import java.io.File
import javax.inject.Inject

/**
 * Verifies a detached .easig signature against the original file.
 * Looks up the signer's public key from the local key directory.
 */
class VerifyFileUseCase @Inject constructor(
    private val hybridCrypto: HybridCryptoEngine,
    private val signingEngine: SigningEngine,
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository
) {
    private val gson = Gson()

    data class VerifyResult(
        val isValid: Boolean,
        val signerUserId: String,
        val message: String
    )

    suspend operator fun invoke(
        signedFileId: String
    ): CryptoResult<VerifyResult> {

        val record = fileRepository.getSignedFile(signedFileId)
            ?: return CryptoResult.Failure(CryptoError.InvalidPackage("Signed file record not found"))

        return invoke(record.filePath, record.signatureFilePath, record.signerId, record.id)
    }

    suspend operator fun invoke(
        filePath: String,
        signatureFilePath: String,
        expectedSignerUserId: String,
        recordId: String? = null
    ): CryptoResult<VerifyResult> {

        // Read original file
        val fileBytes = try {
            File(filePath).readBytes()
        } catch (e: Exception) {
            return CryptoResult.Failure(CryptoError.IOError("Failed to read file: $filePath", e))
        }

        // Parse signature sidecar
        val sigData = try {
            val json = File(signatureFilePath).readText()
            gson.fromJson(json, SignatureFileData::class.java)
        } catch (e: Exception) {
            return CryptoResult.Failure(CryptoError.InvalidPackage("Failed to parse signature file: ${e.message}"))
        }

        // Resolve signer's public key
        val signerBundle = userRepository.getKeyBundle(expectedSignerUserId)
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Signer's public key bundle not found for $expectedSignerUserId"))

        val signerPubKey = signingEngine.publicKeyFromBytes(signerBundle.signingPublicKeyBytes).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Failed to decode signer's EC public key"))

        // Reconstruct DetachedSignature
        val signatureBytes = sigData.signatureHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val dataHash = sigData.dataHashHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val detachedSig = com.example.encryptaction.core.crypto.DetachedSignature(
            signerKeyId = sigData.signerKeyId,
            signatureBytes = signatureBytes,
            dataHash = dataHash
        )

        val isValid = hybridCrypto.verifyDetached(fileBytes, detachedSig, signerPubKey).getOrNull() ?: false

        // Update DB record if we have an ID
        if (recordId != null) {
            val existing = fileRepository.getSignedFile(recordId)
            if (existing != null) {
                fileRepository.updateSignedFile(existing.copy(isVerified = isValid))
            }
        }

        val message = when {
            isValid -> "Signature valid — file is authentic and unmodified"
            else -> "Signature INVALID — file may have been tampered with or signed by a different key"
        }

        return CryptoResult.Success(
            VerifyResult(
                isValid = isValid,
                signerUserId = sigData.signerKeyId,
                message = message
            )
        )
    }

    private data class SignatureFileData(
        val version: Int,
        val signerKeyId: String,
        val signatureHex: String,
        val dataHashHex: String,
        val signedAt: Long
    )
}
