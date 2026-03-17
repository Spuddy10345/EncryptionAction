package com.example.encryptaction.data.repository

import com.example.encryptaction.data.local.dao.KeyBundleDao
import com.example.encryptaction.data.local.dao.UserDao
import com.example.encryptaction.data.local.mapper.toDomain
import com.example.encryptaction.data.local.mapper.toEntity
import com.example.encryptaction.domain.model.User
import com.example.encryptaction.domain.model.UserKeyBundle
import com.example.encryptaction.domain.model.UserRole
import com.example.encryptaction.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val keyBundleDao: KeyBundleDao
) : UserRepository {

    override fun observeUsers(): Flow<List<User>> =
        combine(userDao.observeAll(), keyBundleDao.observeAll()) { users, bundles ->
            val bundleMap = bundles.associateBy { it.userId }
            users.map { entity ->
                entity.toDomain(bundleMap[entity.id]?.toDomain())
            }
        }

    override suspend fun getUserById(id: String): User? {
        val entity = userDao.getById(id) ?: return null
        val bundle = keyBundleDao.getByUserId(id)?.toDomain()
        return entity.toDomain(bundle)
    }

    override suspend fun getUserByUsername(username: String): User? {
        val entity = userDao.getByUsername(username) ?: return null
        val bundle = keyBundleDao.getByUserId(entity.id)?.toDomain()
        return entity.toDomain(bundle)
    }

    override suspend fun createUser(
        username: String,
        displayName: String,
        email: String,
        passwordHash: String,
        role: UserRole
    ): User {
        val user = User(
            id = UUID.randomUUID().toString(),
            username = username,
            displayName = displayName,
            email = email,
            role = role,
            keyBundle = null,
            createdAt = Instant.now()
        )
        userDao.insert(user.toEntity(passwordHash))
        return user
    }

    override suspend fun updateUser(user: User): User {
        val existing = userDao.getById(user.id) ?: error("User ${user.id} not found")
        userDao.update(user.toEntity(existing.passwordHash))
        return user
    }

    override suspend fun deleteUser(id: String) {
        userDao.deleteById(id)
    }

    override suspend fun saveKeyBundle(bundle: UserKeyBundle) {
        keyBundleDao.insertOrReplace(bundle.toEntity())
    }

    override suspend fun getKeyBundle(userId: String): UserKeyBundle? =
        keyBundleDao.getByUserId(userId)?.toDomain()

    override fun observeKeyBundles(): Flow<List<UserKeyBundle>> =
        keyBundleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun verifyPassword(userId: String, passwordHash: String): Boolean {
        val stored = userDao.getPasswordHash(userId) ?: return false
        return stored == passwordHash
    }

    override suspend fun updatePasswordHash(userId: String, newPasswordHash: String) {
        userDao.updatePasswordHash(userId, newPasswordHash)
    }
}
