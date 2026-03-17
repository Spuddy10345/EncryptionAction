package com.example.encryptaction.core.crypto

object CryptoConstants {
    // Symmetric
    const val AES_ALGORITHM = "AES"
    const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    const val AES_KEY_SIZE_BITS = 256
    const val GCM_IV_LENGTH_BYTES = 12
    const val GCM_TAG_LENGTH_BITS = 128

    // Asymmetric
    const val RSA_ALGORITHM = "RSA"
    const val RSA_KEY_SIZE_BITS = 4096
    const val RSA_OAEP_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

    // EC / Signing
    const val EC_ALGORITHM = "EC"
    const val EC_CURVE = "P-256"
    const val ECDSA_SIGNATURE_ALGORITHM = "SHA256withECDSA"

    // Hashing
    const val HASH_ALGORITHM = "SHA-256"

    // Android Keystore
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    // Key alias prefixes (stored in Keystore)
    const val KEY_ALIAS_SIGN_PREFIX = "ea_sign_"
    const val KEY_ALIAS_ENCRYPT_PREFIX = "ea_enc_"

    // File format magic bytes for encrypted packages
    val ENCRYPTED_PACKAGE_MAGIC = byteArrayOf(0x45, 0x41, 0x45, 0x50) // "EAEP"
    const val ENCRYPTED_PACKAGE_VERSION: Byte = 0x01

    // Max file size for in-memory processing (50 MB); larger files use streaming
    const val IN_MEMORY_THRESHOLD_BYTES = 50 * 1024 * 1024L
}
