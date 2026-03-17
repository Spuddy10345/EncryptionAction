package com.example.encryptaction.domain.usecase.auth

import com.example.encryptaction.core.crypto.CryptoError
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.core.crypto.getOrNull
import com.example.encryptaction.domain.model.User
import com.example.encryptaction.domain.model.UserRole
import com.example.encryptaction.domain.repository.UserRepository
import com.example.encryptaction.domain.usecase.key.GenerateUserKeysUseCase
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Registers a new user and immediately generates their key pairs.
 * The first user registered is promoted to ADMIN automatically.
 */
class RegisterUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val generateKeys: GenerateUserKeysUseCase
) {
    data class RegisterResult(val user: User, val fingerprintForDisplay: String)

    suspend operator fun invoke(
        username: String,
        displayName: String,
        email: String,
        password: String,
        role: UserRole = UserRole.MEMBER
    ): CryptoResult<RegisterResult> {

        // Check username availability
        if (userRepository.getUserByUsername(username) != null) {
            return CryptoResult.Failure(CryptoError.KeyGenerationFailed("Username '$username' is already taken"))
        }

        val passwordHash = hashPassword(password)
        val user = userRepository.createUser(username, displayName, email, passwordHash, role)

        // Generate keys for the new user
        val keyBundle = generateKeys(user.id).getOrNull()
            ?: return CryptoResult.Failure(CryptoError.KeyGenerationFailed("Key generation failed for new user"))

        return CryptoResult.Success(
            RegisterResult(user = user, fingerprintForDisplay = keyBundle.fingerprint)
        )
    }

    private fun hashPassword(password: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
