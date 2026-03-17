package com.example.encryptaction.core.crypto

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.security.PrivateKey
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full hybrid encryption workflow as required by the PKI design:
 *
 *   ENCRYPT + SIGN:
 *     1. Generate a random AES-256 session key
 *     2. Encrypt the file content with AES-256-GCM
 *     3. Wrap the AES key with the recipient's RSA-4096 public key (OAEP)
 *     4. Compute SHA-256 of the encrypted payload
 *     5. Sign the hash with the sender's EC private key (ECDSA)
 *     6. Bundle everything into an [EncryptedPackage]
 *
 *   VERIFY + DECRYPT:
 *     1. Verify the sender's signature against the payload hash
 *     2. Unwrap the AES key using the recipient's RSA private key
 *     3. Decrypt the file content
 *
 * Package binary layout (after magic + version header):
 *   [4 bytes: wrappedKey length][wrappedKey bytes]
 *   [4 bytes: signature length][signature bytes]
 *   [4 bytes: senderSigningKeyId length][senderSigningKeyId bytes]
 *   [remaining bytes: IV + ciphertext]
 */
@Singleton
class HybridCryptoEngine @Inject constructor(
    private val aes: AesCryptoEngine,
    private val rsa: RsaCryptoEngine,
    private val signing: SigningEngine
) {

    /**
     * Encrypts [plaintext] for [recipientEncryptionPublicKey] and signs it
     * with [senderSigningPrivateKey].
     *
     * @param senderKeyId  Identifier stored in the package so the recipient knows
     *                     whose signing key to use for verification.
     */
    fun encryptAndSign(
        plaintext: ByteArray,
        recipientEncryptionPublicKey: PublicKey,
        senderSigningPrivateKey: PrivateKey,
        senderKeyId: String
    ): CryptoResult<ByteArray> {
        // 1. Generate AES session key
        val aesKey = aes.generateAesKey().getOrNull()
            ?: return CryptoResult.Failure(CryptoError.EncryptionFailed("AES key generation failed"))

        // 2. Encrypt plaintext
        val encryptedPayload = aes.encrypt(plaintext, aesKey).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.EncryptionFailed("AES encryption failed"))

        // 3. Wrap AES key with recipient's RSA public key
        val wrappedKey = rsa.wrapKey(aesKey.encoded, recipientEncryptionPublicKey).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.EncryptionFailed("RSA key wrap failed"))

        // 4. Sign hash of the encrypted payload
        val payloadHash = signing.hash(encryptedPayload)
        val signature = signing.signHash(payloadHash, senderSigningPrivateKey).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.SigningFailed("Signing failed"))

        // 5. Serialise into package bytes
        return try {
            val senderKeyIdBytes = senderKeyId.toByteArray(Charsets.UTF_8)
            val out = ByteArrayOutputStream()
            val dos = DataOutputStream(out)

            // Magic + version
            dos.write(CryptoConstants.ENCRYPTED_PACKAGE_MAGIC)
            dos.writeByte(CryptoConstants.ENCRYPTED_PACKAGE_VERSION.toInt())

            // Fields
            dos.writeInt(wrappedKey.size)
            dos.write(wrappedKey)

            dos.writeInt(signature.size)
            dos.write(signature)

            dos.writeInt(senderKeyIdBytes.size)
            dos.write(senderKeyIdBytes)

            // Payload last (remainder of stream)
            dos.write(encryptedPayload)
            dos.flush()

            CryptoResult.Success(out.toByteArray())
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.IOError("Failed to serialise encrypted package", e))
        }
    }

    /**
     * Verifies and decrypts a package produced by [encryptAndSign].
     *
     * @param packageBytes              Raw bytes of the encrypted package
     * @param recipientDecryptionPrivKey Recipient's RSA private key (to unwrap AES key)
     * @param senderSigningPublicKey    Sender's EC public key (to verify signature)
     */
    fun verifyAndDecrypt(
        packageBytes: ByteArray,
        recipientDecryptionPrivKey: PrivateKey,
        senderSigningPublicKey: PublicKey
    ): CryptoResult<DecryptedPackage> {
        return try {
            val buf = ByteBuffer.wrap(packageBytes)

            // Validate magic
            val magic = ByteArray(4).also { buf.get(it) }
            if (!magic.contentEquals(CryptoConstants.ENCRYPTED_PACKAGE_MAGIC)) {
                return CryptoResult.Failure(CryptoError.InvalidPackage("Not a valid EncryptAction package"))
            }

            // Validate version
            val version = buf.get()
            if (version != CryptoConstants.ENCRYPTED_PACKAGE_VERSION) {
                return CryptoResult.Failure(CryptoError.InvalidPackage("Unsupported package version: $version"))
            }

            // Read wrapped key
            val wrappedKeyLen = buf.int
            val wrappedKey = ByteArray(wrappedKeyLen).also { buf.get(it) }

            // Read signature
            val sigLen = buf.int
            val signature = ByteArray(sigLen).also { buf.get(it) }

            // Read sender key ID
            val senderKeyIdLen = buf.int
            val senderKeyId = ByteArray(senderKeyIdLen).also { buf.get(it) }
                .toString(Charsets.UTF_8)

            // Read encrypted payload (remainder)
            val encryptedPayload = ByteArray(buf.remaining()).also { buf.get(it) }

            // 1. Verify signature before decrypting anything
            val payloadHash = signing.hash(encryptedPayload)
            val sigValid = signing.verify(payloadHash, signature, senderSigningPublicKey)
            if (sigValid is CryptoResult.Failure) return sigValid
            if ((sigValid as CryptoResult.Success).data == false) {
                return CryptoResult.Failure(CryptoError.VerificationFailed("Signature verification failed — file may be tampered or sender identity mismatch"))
            }

            // 2. Unwrap AES key
            val aesKeyBytes = rsa.unwrapKey(wrappedKey, recipientDecryptionPrivKey).getOrNull()
                ?: return CryptoResult.Failure(CryptoError.DecryptionFailed("AES key unwrap failed"))

            // 3. Decrypt payload
            val aesKey = aes.keyFromBytes(aesKeyBytes)
            val plaintext = aes.decrypt(encryptedPayload, aesKey).getOrNull()
                ?: return CryptoResult.Failure(CryptoError.DecryptionFailed("AES-GCM decryption failed"))

            CryptoResult.Success(
                DecryptedPackage(
                    plaintext = plaintext,
                    senderKeyId = senderKeyId,
                    signatureVerified = true
                )
            )
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.IOError("Failed to parse encrypted package", e))
        }
    }

    /**
     * Signs [data] without encrypting — used for detached file signatures.
     */
    fun sign(
        data: ByteArray,
        signerPrivateKey: PrivateKey,
        signerKeyId: String
    ): CryptoResult<DetachedSignature> {
        val hash = signing.hash(data)
        return signing.signHash(hash, signerPrivateKey).let { result ->
            when (result) {
                is CryptoResult.Success -> CryptoResult.Success(
                    DetachedSignature(
                        signerKeyId = signerKeyId,
                        signatureBytes = result.data,
                        dataHash = hash
                    )
                )
                is CryptoResult.Failure -> result
            }
        }
    }

    /**
     * Verifies a [DetachedSignature] against [data].
     */
    fun verifyDetached(
        data: ByteArray,
        detachedSig: DetachedSignature,
        signerPublicKey: PublicKey
    ): CryptoResult<Boolean> {
        val hash = signing.hash(data)
        if (!hash.contentEquals(detachedSig.dataHash)) {
            return CryptoResult.Failure(CryptoError.VerificationFailed("File hash mismatch — file has been modified"))
        }
        return signing.verify(hash, detachedSig.signatureBytes, signerPublicKey)
    }
}

data class DecryptedPackage(
    val plaintext: ByteArray,
    val senderKeyId: String,
    val signatureVerified: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DecryptedPackage
        return signatureVerified == other.signatureVerified &&
                senderKeyId == other.senderKeyId &&
                plaintext.contentEquals(other.plaintext)
    }

    override fun hashCode(): Int {
        var result = plaintext.contentHashCode()
        result = 31 * result + senderKeyId.hashCode()
        result = 31 * result + signatureVerified.hashCode()
        return result
    }
}

data class DetachedSignature(
    val signerKeyId: String,
    val signatureBytes: ByteArray,
    val dataHash: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DetachedSignature
        return signerKeyId == other.signerKeyId &&
                signatureBytes.contentEquals(other.signatureBytes) &&
                dataHash.contentEquals(other.dataHash)
    }

    override fun hashCode(): Int {
        var result = signerKeyId.hashCode()
        result = 31 * result + signatureBytes.contentHashCode()
        result = 31 * result + dataHash.contentHashCode()
        return result
    }
}
