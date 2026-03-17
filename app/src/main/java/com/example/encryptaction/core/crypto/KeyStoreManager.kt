package com.example.encryptaction.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages cryptographic keys backed by the Android Keystore.
 *
 * Two key pairs per user identity are stored:
 *  - Encryption key pair  (RSA-4096, OAEP)  — used to wrap/unwrap AES file keys
 *  - Signing key pair     (EC P-256, ECDSA)  — used to sign and verify packages
 *
 * Keys are identified by a user alias so multiple local identities can coexist.
 */
@Singleton
class KeyStoreManager @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(CryptoConstants.KEYSTORE_PROVIDER).also {
        it.load(null)
    }

    // -------------------------------------------------------------------------
    // Key generation
    // -------------------------------------------------------------------------

    /**
     * Generates (or regenerates) both key pairs for [userAlias].
     * If keys already exist for this alias they are replaced.
     */
    fun generateKeyPairs(userAlias: String): CryptoResult<Unit> {
        return try {
            generateEncryptionKeyPair(encryptionAlias(userAlias))
            generateSigningKeyPair(signingAlias(userAlias))
            CryptoResult.Success(Unit)
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.KeyGenerationFailed("Failed to generate key pairs for $userAlias", e))
        }
    }

    private fun generateEncryptionKeyPair(alias: String) {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(CryptoConstants.RSA_KEY_SIZE_BITS)
            .setAlgorithmParameterSpec(
                java.security.spec.RSAKeyGenParameterSpec(
                    CryptoConstants.RSA_KEY_SIZE_BITS,
                    java.math.BigInteger.valueOf(65537)
                )
            )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setUserAuthenticationRequired(false)
            .build()

        KeyPairGenerator.getInstance(
            CryptoConstants.RSA_ALGORITHM,
            CryptoConstants.KEYSTORE_PROVIDER
        ).apply {
            initialize(spec)
            generateKeyPair()
        }
    }

    private fun generateSigningKeyPair(alias: String) {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec(CryptoConstants.EC_CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .build()

        KeyPairGenerator.getInstance(
            CryptoConstants.EC_ALGORITHM,
            CryptoConstants.KEYSTORE_PROVIDER
        ).apply {
            initialize(spec)
            generateKeyPair()
        }
    }

    // -------------------------------------------------------------------------
    // Key retrieval
    // -------------------------------------------------------------------------

    fun getEncryptionPublicKey(userAlias: String): CryptoResult<PublicKey> =
        getPublicKey(encryptionAlias(userAlias))

    fun getEncryptionPrivateKey(userAlias: String): CryptoResult<PrivateKey> =
        getPrivateKey(encryptionAlias(userAlias))

    fun getSigningPublicKey(userAlias: String): CryptoResult<PublicKey> =
        getPublicKey(signingAlias(userAlias))

    fun getSigningPrivateKey(userAlias: String): CryptoResult<PrivateKey> =
        getPrivateKey(signingAlias(userAlias))

    fun getEncryptionKeyPair(userAlias: String): CryptoResult<KeyPair> {
        val pub = getEncryptionPublicKey(userAlias).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Encryption public key not found for $userAlias"))
        val priv = getEncryptionPrivateKey(userAlias).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Encryption private key not found for $userAlias"))
        return CryptoResult.Success(KeyPair(pub, priv))
    }

    fun getSigningKeyPair(userAlias: String): CryptoResult<KeyPair> {
        val pub = getSigningPublicKey(userAlias).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Signing public key not found for $userAlias"))
        val priv = getSigningPrivateKey(userAlias).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("Signing private key not found for $userAlias"))
        return CryptoResult.Success(KeyPair(pub, priv))
    }

    // -------------------------------------------------------------------------
    // Key existence / deletion
    // -------------------------------------------------------------------------

    fun hasKeyPairs(userAlias: String): Boolean =
        keyStore.containsAlias(encryptionAlias(userAlias)) &&
                keyStore.containsAlias(signingAlias(userAlias))

    fun deleteKeyPairs(userAlias: String): CryptoResult<Unit> {
        return try {
            if (keyStore.containsAlias(encryptionAlias(userAlias))) {
                keyStore.deleteEntry(encryptionAlias(userAlias))
            }
            if (keyStore.containsAlias(signingAlias(userAlias))) {
                keyStore.deleteEntry(signingAlias(userAlias))
            }
            CryptoResult.Success(Unit)
        } catch (e: Exception) {
            CryptoResult.Failure(CryptoError.KeyGenerationFailed("Failed to delete keys for $userAlias", e))
        }
    }

    // -------------------------------------------------------------------------
    // Public key export (DER bytes — suitable for sharing / storing in DB)
    // -------------------------------------------------------------------------

    fun exportEncryptionPublicKeyBytes(userAlias: String): CryptoResult<ByteArray> {
        return when (val result = getEncryptionPublicKey(userAlias)) {
            is CryptoResult.Success -> CryptoResult.Success(result.data.encoded)
            is CryptoResult.Failure -> result
        }
    }

    fun exportSigningPublicKeyBytes(userAlias: String): CryptoResult<ByteArray> {
        return when (val result = getSigningPublicKey(userAlias)) {
            is CryptoResult.Success -> CryptoResult.Success(result.data.encoded)
            is CryptoResult.Failure -> result
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun getPublicKey(alias: String): CryptoResult<PublicKey> {
        val cert = keyStore.getCertificate(alias)
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("No certificate for alias $alias"))
        return CryptoResult.Success(cert.publicKey)
    }

    private fun getPrivateKey(alias: String): CryptoResult<PrivateKey> {
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("No private key for alias $alias"))
        return CryptoResult.Success(entry.privateKey)
    }

    private fun encryptionAlias(userAlias: String) = "${CryptoConstants.KEY_ALIAS_ENCRYPT_PREFIX}$userAlias"
    private fun signingAlias(userAlias: String) = "${CryptoConstants.KEY_ALIAS_SIGN_PREFIX}$userAlias"
}
