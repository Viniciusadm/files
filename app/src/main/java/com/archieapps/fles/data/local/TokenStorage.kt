package com.archieapps.fles.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth")

class TokenStorage(private val context: Context) {
    private val tokenKey = stringPreferencesKey("auth_token")

    suspend fun getToken(): String? {
        return context.authDataStore.data
            .map { preferences -> preferences[tokenKey] }
            .first()
    }

    suspend fun setToken(token: String?) {
        context.authDataStore.edit { preferences ->
            if (token == null) {
                preferences.remove(tokenKey)
            } else {
                preferences[tokenKey] = token
            }
        }
    }

    suspend fun clearToken() {
        setToken(null)
    }
}
