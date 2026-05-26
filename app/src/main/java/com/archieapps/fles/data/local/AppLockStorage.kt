package com.archieapps.fles.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appLockDataStore by preferencesDataStore(name = "app_lock")

class AppLockStorage(private val context: Context) {
    private val enabledKey = booleanPreferencesKey("enabled")

    val flow: Flow<Boolean> = context.appLockDataStore.data.map { it[enabledKey] ?: false }

    suspend fun isEnabled(): Boolean = flow.first()

    suspend fun setEnabled(value: Boolean) {
        context.appLockDataStore.edit { it[enabledKey] = value }
    }
}
