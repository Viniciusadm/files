package com.archieapps.fles.data.repository

import com.archieapps.fles.data.local.TokenStorage
import com.archieapps.fles.data.local.db.FlesDatabase
import com.archieapps.fles.data.local.db.SessionEntity
import com.archieapps.fles.data.local.db.UserEntity
import com.archieapps.fles.data.local.db.nowMs
import com.archieapps.fles.data.remote.ApiService
import com.archieapps.fles.data.remote.LoginRequest
import com.archieapps.fles.data.remote.LoginResponse
import com.archieapps.fles.data.remote.requireBody
import com.archieapps.fles.ui.model.User

class AuthRepository(
    private val apiService: ApiService,
    private val tokenStorage: TokenStorage,
    private val database: FlesDatabase,
    private val secureFoldersRepository: SecureFoldersRepository,
    private val cryptoRepository: CryptoRepository
) {
    suspend fun login(email: String, password: String): LoginResponse {
        val response = apiService.login(LoginRequest(email, password)).requireBody()
        tokenStorage.setToken(response.token)
        response.user?.let { user ->
            val now = nowMs()
            val localId = user.id ?: user.email
            database.userDao().upsert(UserEntity(localId, user.id, user.name, user.email, now))
            database.sessionDao().upsert(SessionEntity("current", localId, now, now))
        }
        return response
    }

    suspend fun logout() {
        secureFoldersRepository.lock()
        if (!tokenStorage.getToken().isNullOrBlank()) {
            runCatching { apiService.logout() }
        }
        tokenStorage.clearToken()
        database.clearAllTables()
        cryptoRepository.clearKey()
    }

    suspend fun getSavedToken(): String? {
        return tokenStorage.getToken()
    }

    suspend fun me(): User {
        val response = apiService.me().requireBody()
        val now = nowMs()
        val localId = response.id ?: response.email
        database.userDao().upsert(UserEntity(localId, response.id, response.name, response.email, now))
        database.sessionDao().upsert(SessionEntity("current", localId, now, now))
        return User(
            id = response.id?.toLongOrNull() ?: 0L,
            name = response.name,
            email = response.email
        )
    }
}
