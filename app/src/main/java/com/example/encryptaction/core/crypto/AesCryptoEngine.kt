package com.example.encryptaction.core.crypto

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles AES-256-GCM symmetric encryption and decryption.
 *
 * The IV is randomly generated per operation and prepended to the ciphertext.
 * Output format: [12-byte IV][ciphertext + 16-byte GCM auth tag]
 */
@Singleton
class AesCryptoEngine @Inject constructor() {

    /**
     * Generates a fresh random AES-256 key.
     */
    fun generateAesKey(): CryptoResult<SecretKey> {
        return try {
            val generator = KeyGenerator.getInstance(CryptoConstants.AES_ALGORITHM)
            generator.init(CryptoConstants.AES_KEY_SIZE_BITS)
            CryptoResult.Success(generator.generateKey())
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.KeyGenerationFailed("AES key generation failed", e))
        }
    }

    /**
     * Encrypts [plaintext] with [key].
     * Returns: [IV (12 bytes)][ciphertext+tag]
     */
    fun encrypt(plaintext: ByteArray, key: SecretKey): CryptoResult<ByteArray> {
        return try {
            val cipher = Cipher.getInstance(CryptoConstants.AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv // GCM generates IV automatically
            val ciphertext = cipher.doFinal(plaintext)
            CryptoResult.Success(iv + ciphertext)
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.EncryptionFailed("AES-GCM encryption failed", e))
        }
    }

    /**
     * Decrypts [ciphertext] (must include prepended IV) with [key].
     */
    fun decrypt(ciphertext: ByteArray, key: SecretKey): CryptoResult<ByteArray> {
        return try {
            if (ciphertext.size <= CryptoConstants.GCM_IV_LENGTH_BYTES) {
                return CryptoResult.Failure(CryptoError.DecryptionFailed("Ciphertext too short"))
            }
            val iv = ciphertext.copyOfRange(0, CryptoConstants.GCM_IV_LENGTH_BYTES)
            val encrypted = ciphertext.copyOfRange(CryptoConstants.GCM_IV_LENGTH_BYTES, ciphertext.size)

            val spec = GCMParameterSpec(CryptoConstants.GCM_TAG_LENGTH_BITS, iv)
            val cipher = Cipher.getInstance(CryptoConstants.AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            CryptoResult.Success(cipher.doFinal(encrypted))
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.DecryptionFailed("AES-GCM decryption failed — wrong key or tampered data", e))
        }
    }

    /**
     * Reconstructs a [SecretKey] from raw [keyBytes].
     */
    fun keyFromBytes(keyBytes: ByteArray): SecretKey =
        SecretKeySpec(keyBytes, CryptoConstants.AES_ALGORITHM)
}
