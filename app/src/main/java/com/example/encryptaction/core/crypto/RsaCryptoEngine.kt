package com.example.encryptaction.core.crypto

import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles RSA-OAEP asymmetric key wrapping and unwrapping.
 *
 * Used exclusively to encrypt/decrypt the AES session key (key encapsulation).
 * Never used directly on file content.
 */
@Singleton
class RsaCryptoEngine @Inject constructor() {

    private val oaepSpec = OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA1,
        PSource.PSpecified.DEFAULT
    )

    /**
     * Wraps (encrypts) [aesKeyBytes] with the recipient's [publicKey].
     */
    fun wrapKey(aesKeyBytes: ByteArray, publicKey: PublicKey): CryptoResult<ByteArray> {
        return try {
            val cipher = Cipher.getInstance(CryptoConstants.RSA_OAEP_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepSpec)
            CryptoResult.Success(cipher.doFinal(aesKeyBytes))
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.EncryptionFailed("RSA key wrap failed", e))
        }
    }

    /**
     * Unwraps (decrypts) [wrappedKeyBytes] with the recipient's [privateKey].
     */
    fun unwrapKey(wrappedKeyBytes: ByteArray, privateKey: PrivateKey): CryptoResult<ByteArray> {
        return try {
            val cipher = Cipher.getInstance(CryptoConstants.RSA_OAEP_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepSpec)
            CryptoResult.Success(cipher.doFinal(wrappedKeyBytes))
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.DecryptionFailed("RSA key unwrap failed — wrong key or corrupt data", e))
        }
    }

    /**
     * Reconstructs a [PublicKey] from DER-encoded [bytes] (X.509 SubjectPublicKeyInfo).
     */
    fun publicKeyFromBytes(bytes: ByteArray): CryptoResult<PublicKey> {
        return try {
            val keyFactory = java.security.KeyFactory.getInstance(CryptoConstants.RSA_ALGORITHM)
            val spec = java.security.spec.X509EncodedKeySpec(bytes)
            CryptoResult.Success(keyFactory.generatePublic(spec))
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.KeyNotFound("Failed to decode RSA public key"))
        }
    }
}
