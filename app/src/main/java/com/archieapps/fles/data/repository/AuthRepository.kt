package com.archieapps.fles.data.repository

import com.archieapps.fles.data.local.TokenStorage
import com.archieapps.fles.data.remote.ApiService
import com.archieapps.fles.data.remote.LoginRequest
import com.archieapps.fles.data.remote.LoginResponse
import com.archieapps.fles.data.remote.requireBody
import com.archieapps.fles.ui.model.User

class AuthRepository(
    private val apiService: ApiService,
    private val tokenStorage: TokenStorage
) {
    suspend fun login(email: String, password: String): LoginResponse {
        val response = apiService.login(LoginRequest(email, password)).requireBody()
        tokenStorage.setToken(response.token)
        return response
    }

    suspend fun logout() {
        if (!tokenStorage.getToken().isNullOrBlank()) {
            runCatching { apiService.logout() }
        }
        tokenStorage.clearToken()
    }

    suspend fun getSavedToken(): String? {
        return tokenStorage.getToken()
    }

    suspend fun me(): User {
        val response = apiService.me().requireBody()
        return User(
            id = response.id?.toLongOrNull() ?: 0L,
            name = response.name,
            email = response.email
        )
    }
}
