package com.example.encryptaction.domain.model

import java.time.Instant

data class Session(
    val userId: String,
    val username: String,
    val role: UserRole,
    val keystoreAlias: String,
    val loggedInAt: Instant
)
