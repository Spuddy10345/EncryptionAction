package com.example.encryptaction.domain.usecase.file

import android.content.Context
import com.example.encryptaction.core.crypto.CryptoError
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.core.crypto.HybridCryptoEngine
import com.example.encryptaction.core.crypto.KeyStoreManager
import com.example.encryptaction.core.crypto.RsaCryptoEngine
import com.example.encryptaction.core.crypto.getOrNull
import com.example.encryptaction.domain.model.EncryptedFile
import com.example.encryptaction.domain.model.EncryptedFileStatus
import com.example.encryptaction.domain.repository.FileRepository
import com.example.encryptaction.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Encrypts a file for a specific recipient using the hybrid scheme:
 *   AES-256-GCM (file content) + RSA-OAEP (key wrap) + ECDSA (signature)
 *
 * Output is a .eaep package file written to the app's private files directory.
 */
class EncryptFileUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hybridCrypto: HybridCryptoEngine,
    private val keyStoreManager: KeyStoreManager,
    private val rsaEngine: RsaCryptoEngine,
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(
        sourceFilePath: String,
        mimeType: String,
        senderUserId: String,
        recipientUserId: String
    ): CryptoResult<EncryptedFile> {

        // 1. Load source file
        val sourceFile = File(sourceFilePath)
        if (!sourceFile.exists()) {
            return CryptoResult.Failure(CryptoError.IOError("Source file not found: $sourceFilePath"))
        }
        val plaintext = try {
            sourceFile.readBytes()
        } catch (e: Exception) {
            return CryptoResult.Failure(CryptoError.IOError("Failed to read source file", e))
        }

        // 2. Compute plaintext hash for integrity record
        val plaintextHash = MessageDigest.getInstance("SHA-256").digest(plaintext)
            .joinToString("") { "%02x".format(it) }

        // 3. Get recipient's encryption public key
        val recipientBundle = userRepository.getKeyBundle(recipientUserId)
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("No key bundle for recipient $recipientUserId — have they shared their public keys?"))

        val recipientEncPublicKey = rsaEngine.publicKeyFromBytes(recipientBundle.encryptionPublicKeyBytes).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Failed to decode recipient's encryption public key"))

        // 4. Get sender's signing private key
        val senderSignPrivKey = keyStoreManager.getSigningPrivateKey(senderUserId).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Sender signing key not found — generate keys first"))

        // 5. Encrypt and sign
        val packageBytes = hybridCrypto.encryptAndSign(
            plaintext = plaintext,
            recipientEncryptionPublicKey = recipientEncPublicKey,
            senderSigningPrivateKey = senderSignPrivKey,
            senderKeyId = senderUserId
        ).getOrNull() ?: return CryptoResult.Failure(CryptoError.EncryptionFailed("Hybrid encrypt+sign failed"))

        // 6. Write package to private app storage
        val outputDir = File(context.filesDir, "encrypted").also { it.mkdirs() }
        val outputFile = File(outputDir, "${UUID.randomUUID()}.eaep")
        try {
            outputFile.writeBytes(packageBytes)
        } catch (e: Exception) {
            return CryptoResult.Failure(CryptoError.IOError("Failed to write encrypted package", e))
        }

        // 7. Save record to database
        val record = EncryptedFile(
            id = UUID.randomUUID().toString(),
            originalName = sourceFile.name,
            mimeType = mimeType,
            originalSizeBytes = plaintext.size.toLong(),
            encryptedFilePath = outputFile.absolutePath,
            senderId = senderUserId,
            recipientId = recipientUserId,
            encryptedAt = Instant.now(),
            status = EncryptedFileStatus.PENDING_SEND,
            plaintextHash = plaintextHash
        )

        return fileRepository.saveEncryptedFile(record).let { CryptoResult.Success(it) }
    }
}
