package com.lowderancorp.inioli.data.auth

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val AUTH_SESSION_DATASTORE = "auth_session"

private val Context.authDataStore by preferencesDataStore(name = AUTH_SESSION_DATASTORE)

class AuthLocalDataSource(private val context: Context) {
    val session: Flow<UserSession?> =
        context.authDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val token = preferences[Keys.accessToken]
                val username = preferences[Keys.username]

                if (token.isNullOrBlank() || username.isNullOrBlank()) {
                    null
                } else {
                    UserSession(
                        accessToken = token,
                        username = username
                    )
                }
            }

    suspend fun saveSession(session: UserSession) {
        context.authDataStore.edit { preferences ->
            preferences[Keys.accessToken] = session.accessToken
            preferences[Keys.username] = session.username
        }
    }

    suspend fun clearSession() {
        context.authDataStore.edit { preferences ->
            preferences.remove(Keys.accessToken)
            preferences.remove(Keys.username)
        }
    }

    suspend fun getSession(): UserSession? {
        return session.first()
    }

    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val username = stringPreferencesKey("username")
    }
}
