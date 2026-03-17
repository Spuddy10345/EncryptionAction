package com.example.encryptaction.domain.usecase.file

import android.content.Context
import com.example.encryptaction.core.crypto.CryptoError
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.core.crypto.HybridCryptoEngine
import com.example.encryptaction.core.crypto.KeyStoreManager
import com.example.encryptaction.core.crypto.getOrNull
import com.example.encryptaction.domain.model.SignedFile
import com.example.encryptaction.domain.repository.FileRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Creates a detached signature for a file.
 * Writes a .easig JSON sidecar file alongside the original.
 * The original file is not modified.
 */
class SignFileUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hybridCrypto: HybridCryptoEngine,
    private val keyStoreManager: KeyStoreManager,
    private val fileRepository: FileRepository
) {
    private val gson = Gson()

    suspend operator fun invoke(
        sourceFilePath: String,
        signerUserId: String
    ): CryptoResult<SignedFile> {

        val sourceFile = File(sourceFilePath)
        if (!sourceFile.exists()) {
            return CryptoResult.Failure(CryptoError.IOError("File not found: $sourceFilePath"))
        }

        val fileBytes = try {
            sourceFile.readBytes()
        } catch (e: Exception) {
            return CryptoResult.Failure(CryptoError.IOError("Failed to read file", e))
        }

        val signerPrivKey = keyStoreManager.getSigningPrivateKey(signerUserId).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Signing key not found for $signerUserId"))

        val detachedSig = hybridCrypto.sign(fileBytes, signerPrivKey, signerUserId).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.SigningFailed("Signing operation failed"))

        // Serialise signature to JSON sidecar
        val sigData = SignatureFileData(
            version = 1,
            signerKeyId = detachedSig.signerKeyId,
            signatureHex = detachedSig.signatureBytes.joinToString("") { "%02x".format(it) },
            dataHashHex = detachedSig.dataHash.joinToString("") { "%02x".format(it) },
            signedAt = Instant.now().toEpochMilli()
        )

        val sigDir = File(context.filesDir, "signatures").also { it.mkdirs() }
        val sigFile = File(sigDir, "${sourceFile.nameWithoutExtension}_${UUID.randomUUID()}.easig")
        try {
            sigFile.writeText(gson.toJson(sigData))
        } catch (e: Exception) {
            return CryptoResult.Failure(CryptoError.IOError("Failed to write signature file", e))
        }

        val record = SignedFile(
            id = UUID.randomUUID().toString(),
            originalName = sourceFile.name,
            filePath = sourceFilePath,
            signerId = signerUserId,
            signatureFilePath = sigFile.absolutePath,
            signedAt = Instant.now(),
            isVerified = null,
            fileHash = sigData.dataHashHex
        )

        fileRepository.saveSignedFile(record)
        return CryptoResult.Success(record)
    }

    private data class SignatureFileData(
        val version: Int,
        val signerKeyId: String,
        val signatureHex: String,
        val dataHashHex: String,
        val signedAt: Long
    )
}
