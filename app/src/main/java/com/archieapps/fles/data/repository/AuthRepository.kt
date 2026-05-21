package com.archieapps.fles.data.repository

import com.archieapps.fles.data.local.TokenStorage
import com.archieapps.fles.data.remote.ApiService
import com.archieapps.fles.data.remote.LoginRequest
import com.archieapps.fles.data.remote.LoginResponse
import com.archieapps.fles.data.remote.UserResponse

class AuthRepository(
    private val apiService: ApiService,
    private val tokenStorage: TokenStorage
) {
    suspend fun login(email: String, password: String): LoginResponse {
        val response = apiService.login(LoginRequest(email, password))
        tokenStorage.setToken(response.token)
        return response
    }

    suspend fun logout() {
        tokenStorage.clearToken()
    }

    suspend fun me(): UserResponse {
        return apiService.me()
    }
}
