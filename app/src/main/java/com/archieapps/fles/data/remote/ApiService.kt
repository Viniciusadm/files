package com.archieapps.fles.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("me")
    suspend fun me(): UserResponse
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String
)

data class UserResponse(
    val id: Long,
    val name: String?,
    val email: String
)
