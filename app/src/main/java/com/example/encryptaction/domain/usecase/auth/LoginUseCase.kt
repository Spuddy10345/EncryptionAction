package com.example.encryptaction.domain.usecase.auth

import com.example.encryptaction.core.crypto.CryptoError
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.domain.model.Session
import com.example.encryptaction.domain.repository.SessionRepository
import com.example.encryptaction.domain.repository.UserRepository
import java.security.MessageDigest
import java.time.Instant
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(username: String, password: String): CryptoResult<Session> {
        val user = userRepository.getUserByUsername(username)
            ?: return CryptoResult.Failure(CryptoError.KeyNotFound("User not found"))

        if (!user.isActive) {
            return CryptoResult.Failure(CryptoError.KeyNotFound("Account is disabled"))
        }

        val passwordHash = hashPassword(password)
        val valid = userRepository.verifyPassword(user.id, passwordHash)
        if (!valid) {
            return CryptoResult.Failure(CryptoError.VerificationFailed("Invalid credentials"))
        }

        val session = Session(
            userId = user.id,
            username = user.username,
            role = user.role,
            keystoreAlias = user.id,
            loggedInAt = Instant.now()
        )
        sessionRepository.saveSession(session)
        return CryptoResult.Success(session)
    }

    private fun hashPassword(password: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
