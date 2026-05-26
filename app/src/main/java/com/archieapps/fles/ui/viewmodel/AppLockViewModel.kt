package com.archieapps.fles.ui.viewmodel

import android.content.Context
import android.os.SystemClock
import com.archieapps.fles.data.local.AppLockStorage
import com.archieapps.fles.security.BiometricAuthenticator
import com.archieapps.fles.security.BiometricAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppLockState(
    val enabled: Boolean = false,
    val unlocked: Boolean = false,
    val availability: BiometricAvailability = BiometricAvailability.Unavailable
)

class AppLockViewModel(
    private val appContext: Context,
    private val storage: AppLockStorage
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(AppLockState())
    val state: StateFlow<AppLockState> = _state.asStateFlow()

    private var backgroundedAt: Long? = null

    init {
        refreshAvailability()
        scope.launch {
            storage.flow.collect { enabled ->
                _state.update { it.copy(enabled = enabled) }
            }
        }
    }

    fun refreshAvailability() {
        val availability = BiometricAuthenticator.canAuthenticate(appContext)
        _state.update { it.copy(availability = availability) }
    }

    fun onAppBackgrounded() {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    fun onAppForegrounded() {
        refreshAvailability()
        val ts = backgroundedAt
        if (ts != null && _state.value.enabled) {
            val elapsed = SystemClock.elapsedRealtime() - ts
            if (elapsed >= LOCK_GRACE_MILLIS) {
                _state.update { it.copy(unlocked = false) }
            }
        }
        backgroundedAt = null
    }

    fun markUnlocked() {
        _state.update { it.copy(unlocked = true) }
    }

    fun resetUnlocked() {
        _state.update { it.copy(unlocked = false) }
    }

    fun setEnabled(value: Boolean): Job = scope.launch {
        storage.setEnabled(value)
        if (value) {
            _state.update { it.copy(unlocked = true) }
        }
    }

    fun clear() {
        scope.cancel()
    }

    companion object {
        const val LOCK_GRACE_MILLIS: Long = 5 * 60 * 1000L
    }
}
