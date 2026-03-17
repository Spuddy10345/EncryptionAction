package com.example.encryptaction.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.encryptaction.domain.model.Session
import com.example.encryptaction.domain.model.UserRole
import com.example.encryptaction.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SessionRepository {

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("session_user_id")
        private val KEY_USERNAME = stringPreferencesKey("session_username")
        private val KEY_ROLE = stringPreferencesKey("session_role")
        private val KEY_KEYSTORE_ALIAS = stringPreferencesKey("session_keystore_alias")
        private val KEY_LOGGED_IN_AT = longPreferencesKey("session_logged_in_at")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("session_is_logged_in")
    }

    override fun observeSession(): Flow<Session?> = dataStore.data.map { prefs ->
        if (prefs[KEY_IS_LOGGED_IN] != true) return@map null
        val userId = prefs[KEY_USER_ID] ?: return@map null
        Session(
            userId = userId,
            username = prefs[KEY_USERNAME] ?: "",
            role = UserRole.valueOf(prefs[KEY_ROLE] ?: UserRole.MEMBER.name),
            keystoreAlias = prefs[KEY_KEYSTORE_ALIAS] ?: userId,
            loggedInAt = Instant.ofEpochMilli(prefs[KEY_LOGGED_IN_AT] ?: 0L)
        )
    }

    override suspend fun getSession(): Session? = observeSession().first()

    override suspend fun saveSession(session: Session) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = session.userId
            prefs[KEY_USERNAME] = session.username
            prefs[KEY_ROLE] = session.role.name
            prefs[KEY_KEYSTORE_ALIAS] = session.keystoreAlias
            prefs[KEY_LOGGED_IN_AT] = session.loggedInAt.toEpochMilli()
            prefs[KEY_IS_LOGGED_IN] = true
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_ROLE)
            prefs.remove(KEY_KEYSTORE_ALIAS)
            prefs.remove(KEY_LOGGED_IN_AT)
            prefs[KEY_IS_LOGGED_IN] = false
        }
    }

    override val isLoggedIn: Boolean
        get() = throw UnsupportedOperationException("Use observeSession() or getSession() instead")
}
