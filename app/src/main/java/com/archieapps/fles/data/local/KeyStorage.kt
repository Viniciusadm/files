package com.archieapps.fles.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.keyDataStore by preferencesDataStore(name = "e2ee")

class KeyStorage(private val context: Context) {
    private val key = stringPreferencesKey("e2ee_key")

    suspend fun getKey(): String? {
        return context.keyDataStore.data
            .map { preferences -> preferences[key] }
            .first()
    }

    suspend fun setKey(value: String?) {
        context.keyDataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }
    }
}
