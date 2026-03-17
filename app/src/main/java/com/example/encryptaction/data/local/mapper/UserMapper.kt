package com.example.encryptaction.data.local.mapper

import android.util.Base64
import com.example.encryptaction.data.local.entity.KeyBundleEntity
import com.example.encryptaction.data.local.entity.UserEntity
import com.example.encryptaction.domain.model.User
import com.example.encryptaction.domain.model.UserKeyBundle
import com.example.encryptaction.domain.model.UserRole
import java.time.Instant

fun UserEntity.toDomain(keyBundle: UserKeyBundle? = null) = User(
    id = id,
    username = username,
    displayName = displayName,
    email = email,
    role = UserRole.valueOf(role),
    keyBundle = keyBundle,
    createdAt = Instant.ofEpochMilli(createdAt),
    isActive = isActive
)

fun User.toEntity(passwordHash: String) = UserEntity(
    id = id,
    username = username,
    displayName = displayName,
    email = email,
    passwordHash = passwordHash,
    role = role.name,
    createdAt = createdAt.toEpochMilli(),
    isActive = isActive
)

fun KeyBundleEntity.toDomain() = UserKeyBundle(
    userId = userId,
    encryptionPublicKeyBytes = Base64.decode(encryptionPublicKeyBase64, Base64.NO_WRAP),
    signingPublicKeyBytes = Base64.decode(signingPublicKeyBase64, Base64.NO_WRAP),
    createdAt = Instant.ofEpochMilli(createdAt),
    fingerprint = fingerprint
)

fun UserKeyBundle.toEntity() = KeyBundleEntity(
    userId = userId,
    encryptionPublicKeyBase64 = Base64.encodeToString(encryptionPublicKeyBytes, Base64.NO_WRAP),
    signingPublicKeyBase64 = Base64.encodeToString(signingPublicKeyBytes, Base64.NO_WRAP),
    createdAt = createdAt.toEpochMilli(),
    fingerprint = fingerprint
)
