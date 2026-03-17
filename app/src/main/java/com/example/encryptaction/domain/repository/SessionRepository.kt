package com.example.encryptaction.domain.repository

import com.example.encryptaction.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeSession(): Flow<Session?>
    suspend fun getSession(): Session?
    suspend fun saveSession(session: Session)
    suspend fun clearSession()
    val isLoggedIn: Boolean
}
