package com.example.encryptaction.core.crypto

/**
 * Sealed wrapper for all crypto operations. Avoids throwing exceptions across
 * coroutine/DI boundaries and keeps error handling explicit at call sites.
 */
sealed class CryptoResult<out T> {
    data class Success<T>(val data: T) : CryptoResult<T>()
    data class Failure(val error: CryptoError) : CryptoResult<Nothing>()
}

sealed class CryptoError(val message: String, val cause: Throwable? = null) {
    class KeyGenerationFailed(msg: String, cause: Throwable? = null) : CryptoError(msg, cause)
    class KeyNotFound(msg: String) : CryptoError(msg)
    class EncryptionFailed(msg: String, cause: Throwable? = null) : CryptoError(msg, cause)
    class DecryptionFailed(msg: String, cause: Throwable? = null) : CryptoError(msg, cause)
    class SigningFailed(msg: String, cause: Throwable? = null) : CryptoError(msg, cause)
    class VerificationFailed(msg: String, cause: Throwable? = null) : CryptoError(msg, cause)
    class InvalidPackage(msg: String) : CryptoError(msg)
    class IOError(msg: String, cause: Throwable? = null) : CryptoError(msg, cause)
}

inline fun <T> CryptoResult<T>.onSuccess(block: (T) -> Unit): CryptoResult<T> {
    if (this is CryptoResult.Success) block(data)
    return this
}

inline fun <T> CryptoResult<T>.onFailure(block: (CryptoError) -> Unit): CryptoResult<T> {
    if (this is CryptoResult.Failure) block(error)
    return this
}

fun <T> CryptoResult<T>.getOrNull(): T? = (this as? CryptoResult.Success)?.data
