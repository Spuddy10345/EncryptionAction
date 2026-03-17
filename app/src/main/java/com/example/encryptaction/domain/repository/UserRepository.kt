package com.example.encryptaction.domain.repository

import com.example.encryptaction.domain.model.User
import com.example.encryptaction.domain.model.UserKeyBundle
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /** Returns all users in the organisation directory. */
    fun observeUsers(): Flow<List<User>>

    suspend fun getUserById(id: String): User?

    suspend fun getUserByUsername(username: String): User?

    /** Creates a new user record. Returns the created User. */
    suspend fun createUser(
        username: String,
        displayName: String,
        email: String,
        passwordHash: String,
        role: com.example.encryptaction.domain.model.UserRole
    ): User

    suspend fun updateUser(user: User): User

    suspend fun deleteUser(id: String)

    /** Stores or replaces the user's public key bundle in the local directory. */
    suspend fun saveKeyBundle(bundle: UserKeyBundle)

    suspend fun getKeyBundle(userId: String): UserKeyBundle?

    /** Returns all key bundles — used to display the org key directory. */
    fun observeKeyBundles(): Flow<List<UserKeyBundle>>

    /** Verifies a user's password. Returns true if correct. */
    suspend fun verifyPassword(userId: String, passwordHash: String): Boolean

    suspend fun updatePasswordHash(userId: String, newPasswordHash: String)
}
