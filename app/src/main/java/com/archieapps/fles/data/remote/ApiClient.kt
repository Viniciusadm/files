package com.archieapps.fles.data.remote

import android.content.Context
import com.archieapps.fles.BuildConfig
import com.archieapps.fles.data.local.TokenStorage
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient(context: Context) {
    val tokenStorage = TokenStorage(context.applicationContext)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStorage))
        // Reabsorve 429/503 (Retry-After) e espaça as chamadas abaixo do limite do
        // backend (throttle:files = 240/min), deixando folga para os downloads do Coil.
        .addInterceptor(RateLimitInterceptor(permitsPerMinute = 180))
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
