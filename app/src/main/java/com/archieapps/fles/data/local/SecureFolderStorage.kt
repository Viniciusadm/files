package com.archieapps.fles.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.archieapps.fles.data.remote.SecureFolderMetaResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.secureFolderDataStore by preferencesDataStore(name = "secure_folders")

class SecureFolderStorage(private val context: Context) {
    private val keySalt = stringPreferencesKey("key_salt")
    private val keyIterations = intPreferencesKey("key_iterations")
    private val keyEnvelope = stringPreferencesKey("key_envelope")
    private val keyVersion = intPreferencesKey("key_version")
    private val pinSetAt = stringPreferencesKey("pin_set_at")

    suspend fun getMeta(): SecureFolderMetaResponse? {
        return context.secureFolderDataStore.data.map { preferences ->
            val salt = preferences[keySalt]
            val iterations = preferences[keyIterations]
            val envelope = preferences[keyEnvelope]
            if (salt == null || iterations == null || envelope == null) {
                null
            } else {
                SecureFolderMetaResponse(
                    configured = true,
                    keySalt = salt,
                    keyIterations = iterations,
                    keyEnvelope = envelope,
                    keyVersion = preferences[keyVersion] ?: 1,
                    pinSetAt = preferences[pinSetAt]
                )
            }
        }.first()
    }

    suspend fun saveMeta(meta: SecureFolderMetaResponse) {
        context.secureFolderDataStore.edit { preferences ->
            if (meta.configured && meta.keySalt != null && meta.keyIterations != null && meta.keyEnvelope != null) {
                preferences[keySalt] = meta.keySalt
                preferences[keyIterations] = meta.keyIterations
                preferences[keyEnvelope] = meta.keyEnvelope
                preferences[keyVersion] = meta.keyVersion ?: 1
                meta.pinSetAt?.let { preferences[pinSetAt] = it }
            }
        }
    }

    suspend fun clear() {
        context.secureFolderDataStore.edit { it.clear() }
    }
}
