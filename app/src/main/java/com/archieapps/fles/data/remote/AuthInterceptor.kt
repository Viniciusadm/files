package com.archieapps.fles.data.remote

import com.archieapps.fles.data.local.TokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenStorage: TokenStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStorage.getToken() }

        val requestBuilder = chain.request()
            .newBuilder()
            .header("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
