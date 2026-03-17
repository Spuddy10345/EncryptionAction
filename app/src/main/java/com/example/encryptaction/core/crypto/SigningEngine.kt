package com.example.encryptaction.core.crypto

import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles ECDSA (P-256 / SHA-256) signing and verification.
 *
 * Signs a SHA-256 digest of the data rather than the raw data, making it
 * suitable for large files without loading everything into memory twice.
 */
@Singleton
class SigningEngine @Inject constructor() {

    /**
     * Signs [data] with [privateKey].
     * Returns the DER-encoded ECDSA signature bytes.
     */
    fun sign(data: ByteArray, privateKey: PrivateKey): CryptoResult<ByteArray> {
        return try {
            val sig = Signature.getInstance(CryptoConstants.ECDSA_SIGNATURE_ALGORITHM)
            sig.initSign(privateKey)
            sig.update(data)
            CryptoResult.Success(sig.sign())
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.SigningFailed("ECDSA signing failed", e))
        }
    }

    /**
     * Signs the SHA-256 [hash] of a file (preferred for large files).
     */
    fun signHash(hash: ByteArray, privateKey: PrivateKey): CryptoResult<ByteArray> =
        sign(hash, privateKey)

    /**
     * Verifies [signature] against [data] using [publicKey].
     * Returns Success(true) if valid, Success(false) if signature doesn't match,
     * Failure if the operation itself fails.
     */
    fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey): CryptoResult<Boolean> {
        return try {
            val sig = Signature.getInstance(CryptoConstants.ECDSA_SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(data)
            CryptoResult.Success(sig.verify(signature))
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.VerificationFailed("ECDSA verification failed", e))
        }
    }

    /**
     * Reconstructs a [PublicKey] from DER-encoded [bytes] (X.509 SubjectPublicKeyInfo).
     */
    fun publicKeyFromBytes(bytes: ByteArray): CryptoResult<PublicKey> {
        return try {
            val keyFactory = java.security.KeyFactory.getInstance(CryptoConstants.EC_ALGORITHM)
            val spec = java.security.spec.X509EncodedKeySpec(bytes)
            CryptoResult.Success(keyFactory.generatePublic(spec))
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.KeyNotFound("Failed to decode EC public key"))
        }
    }

    /**
     * Computes SHA-256 hash of [data].
     */
    fun hash(data: ByteArray): ByteArray {
        return MessageDigest.getInstance(CryptoConstants.HASH_ALGORITHM).digest(data)
    }
}
