package com.example.encryptaction.domain.usecase.file

import android.content.Context
import com.example.encryptaction.core.crypto.CryptoError
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.core.crypto.HybridCryptoEngine
import com.example.encryptaction.core.crypto.KeyStoreManager
import com.example.encryptaction.core.crypto.SigningEngine
import com.example.encryptaction.core.crypto.getOrNull
import com.example.encryptaction.domain.model.EncryptedFile
import com.example.encryptaction.domain.model.EncryptedFileStatus
import com.example.encryptaction.domain.repository.FileRepository
import com.example.encryptaction.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Decrypts a .eaep package:
 *   1. Verifies the sender's ECDSA signature
 *   2. Unwraps the AES key using the recipient's RSA private key
 *   3. Decrypts the file content
 *   4. Verifies the plaintext hash matches the original record
 *
 * Writes the decrypted file to the app's private decrypted/ directory.
 */
class DecryptFileUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hybridCrypto: HybridCryptoEngine,
    private val keyStoreManager: KeyStoreManager,
    private val signingEngine: SigningEngine,
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository
) {
    data class DecryptResult(
        val outputFilePath: String,
        val originalName: String,
        val signatureVerified: Boolean,
        val senderUserId: String
    )

    suspend operator fun invoke(
        encryptedFileId: String,
        recipientUserId: String
    ): CryptoResult<DecryptResult> {

        // 1. Load file record
        val record = fileRepository.getEncryptedFile(encryptedFileId)
            ?: return CryptoResult.Failure(CryptoError.InvalidPackage("Encrypted file record not found"))

        // 2. Read .eaep package bytes
        val packageFile = File(record.encryptedFilePath)
        if (!packageFile.exists()) {
            return CryptoResult.Failure(CryptoError.IOError("Encrypted package file missing from storage"))
        }
        val packageBytes = try {
            packageFile.readBytes()
        } catch (e: Exception) {
            return CryptoResult.Failure(CryptoError.IOError("Failed to read encrypted package", e))
        }

        // 3. Get recipient's RSA private key (decryption)
        val recipientPrivKey = keyStoreManager.getEncryptionPrivateKey(recipientUserId).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Recipient private key not found — this file was not encrypted for this account"))

        // 4. Get sender's EC public key (verification)
        //    The sender key ID is embedded in the package — we resolve it from the DB
        val senderBundle = userRepository.getKeyBundle(record.senderId)
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Sender's public key bundle not found — import their keys first"))

        val senderSignPublicKey = signingEngine.publicKeyFromBytes(senderBundle.signingPublicKeyBytes).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Failed to decode sender's signing public key"))

        // 5. Verify + Decrypt
        val decrypted = hybridCrypto.verifyAndDecrypt(
            packageBytes = packageBytes,
            recipientDecryptionPrivKey = recipientPrivKey,
            senderSigningPublicKey = senderSignPublicKey
        ).getOrNull() ?: return CryptoResult.Failure(CryptoError.DecryptionFailed("Decryption or verification failed"))

        // 6. Verify plaintext hash matches stored record
        val actualHash = MessageDigest.getInstance("SHA-256").digest(decrypted.plaintext)
            .joinToString("") { "%02x".format(it) }

        if (actualHash != record.plaintextHash) {
            return CryptoResult.Failure(
                CryptoError.VerificationFailed("Plaintext hash mismatch — file integrity compromised")
            )
        }

        // 7. Write decrypted file to private storage
        val outputDir = File(context.filesDir, "decrypted").also { it.mkdirs() }
        val outputFile = File(outputDir, record.originalName)
        try {
            outputFile.writeBytes(decrypted.plaintext)
        } catch (e: Exception) {
            return CryptoResult.Failure(CryptoError.IOError("Failed to write decrypted file", e))
        }

        // 8. Update record status
        fileRepository.updateEncryptedFile(record.copy(status = EncryptedFileStatus.DECRYPTED))

        return CryptoResult.Success(
            DecryptResult(
                outputFilePath = outputFile.absolutePath,
                originalName = record.originalName,
                signatureVerified = decrypted.signatureVerified,
                senderUserId = record.senderId
            )
        )
    }
}
