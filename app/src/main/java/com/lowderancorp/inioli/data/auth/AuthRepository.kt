package com.lowderancorp.inioli.data.auth

import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val localDataSource: AuthLocalDataSource,
    private val remoteDataSource: AuthRemoteDataSource
) {
    val session: Flow<UserSession?> = localDataSource.session

    suspend fun login(username: String, password: String): UserSession {
        val session = remoteDataSource.login(username = username, password = password)
        localDataSource.saveSession(session)
        return session
    }

    suspend fun logout() {
        localDataSource.clearSession()
    }
}
