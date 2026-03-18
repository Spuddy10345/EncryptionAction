package com.example.encryptaction.domain.usecase.file

import android.content.Context
import com.example.encryptaction.core.crypto.CryptoConstants
import com.example.encryptaction.core.crypto.CryptoError
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.core.crypto.HybridCryptoEngine
import com.example.encryptaction.core.crypto.KeyStoreManager
import com.example.encryptaction.core.crypto.SigningEngine
import com.example.encryptaction.core.crypto.getOrNull
import com.example.encryptaction.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * Decrypts a .eaep package that was obtained externally (not tracked in the local DB).
 *
 *   1. Parses the senderKeyId from the package header
 *   2. Looks up the sender's signing public key in the key bundle store
 *   3. Verifies the ECDSA signature
 *   4. Unwraps the AES key with the recipient's RSA private key
 *   5. Decrypts the file content
 *   6. Writes the output to the app's private decrypted/ directory
 */
class DecryptExternalFileUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hybridCrypto: HybridCryptoEngine,
    private val keyStoreManager: KeyStoreManager,
    private val signingEngine: SigningEngine,
    private val userRepository: UserRepository
) {
    data class DecryptResult(
        val outputFilePath: String,
        val suggestedName: String,
        val signatureVerified: Boolean,
        val senderKeyId: String
    )

    suspend operator fun invoke(
        packageFilePath: String,
        recipientUserId: String
    ): CryptoResult<DecryptResult> {

        // 1. Read package bytes
        val packageFile = File(packageFilePath)
        if (!packageFile.exists()) {
            return CryptoResult.Failure(CryptoError.IOError("File not found: $packageFilePath"))
        }
        val packageBytes = try {
            packageFile.readBytes()
        } catch (e: Exception) {
            return CryptoResult.Failure(CryptoError.IOError("Failed to read file", e))
        }

        // 2. Peek at the package header to extract senderKeyId before full decryption
        val senderKeyId = peekSenderKeyId(packageBytes)
            ?: return CryptoResult.Failure(CryptoError.InvalidPackage("Not a valid EncryptAction package (.eaep)"))

        // 3. Get recipient's RSA private key
        val recipientPrivKey = keyStoreManager.getEncryptionPrivateKey(recipientUserId).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Your decryption key was not found in the Keystore"))

        // 4. Look up sender's signing public key
        val senderBundle = userRepository.getKeyBundle(senderKeyId)
            ?: return CryptoResult.Failure(
                CryptoError.KeyNotFound("Sender's public key not found — import their keys first (key ID: $senderKeyId)")
            )
        val senderSignPublicKey = signingEngine.publicKeyFromBytes(senderBundle.signingPublicKeyBytes).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Failed to decode sender's signing public key"))

        // 5. Verify signature + decrypt
        val hybridResult = hybridCrypto.verifyAndDecrypt(
            packageBytes = packageBytes,
            recipientDecryptionPrivKey = recipientPrivKey,
            senderSigningPublicKey = senderSignPublicKey
        )
        if (hybridResult is CryptoResult.Failure) return hybridResult
        val decrypted = (hybridResult as CryptoResult.Success).data

        // 6. Derive output filename (strip .eaep suffix if present)
        val suggestedName = packageFile.name
            .let { if (it.endsWith(".eaep", ignoreCase = true)) it.dropLast(5) else it }
            .ifBlank { "decrypted_${System.currentTimeMillis()}" }

        // 7. Write decrypted file to private storage
        val outputDir = File(context.filesDir, "decrypted").also { it.mkdirs() }
        val outputFile = File(outputDir, suggestedName)
        try {
            outputFile.writeBytes(decrypted.plaintext)
        } catch (e: Exception) {
            return CryptoResult.Failure(CryptoError.IOError("Failed to write decrypted file", e))
        }

        return CryptoResult.Success(
            DecryptResult(
                outputFilePath = outputFile.absolutePath,
                suggestedName = suggestedName,
                signatureVerified = decrypted.signatureVerified,
                senderKeyId = senderKeyId
            )
        )
    }

    /**
     * Reads just the senderKeyId field from the package header without performing any
     * cryptographic operations. Returns null if the bytes are not a valid .eaep package.
     *
     * Layout: [magic 4B][version 1B][wrappedKeyLen 4B][wrappedKey][sigLen 4B][sig][senderKeyIdLen 4B][senderKeyId]…
     */
    private fun peekSenderKeyId(packageBytes: ByteArray): String? {
        return try {
            val buf = ByteBuffer.wrap(packageBytes)
            val magic = ByteArray(4).also { buf.get(it) }
            if (!magic.contentEquals(CryptoConstants.ENCRYPTED_PACKAGE_MAGIC)) return null
            buf.get() // version
            val wrappedKeyLen = buf.int
            buf.position(buf.position() + wrappedKeyLen)
            val sigLen = buf.int
            buf.position(buf.position() + sigLen)
            val senderKeyIdLen = buf.int
            ByteArray(senderKeyIdLen).also { buf.get(it) }.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
