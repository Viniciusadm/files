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
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
