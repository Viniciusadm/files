package com.archieapps.fles.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GlobalSyncState {
    Idle,
    Syncing,
    Offline,
    Failed,
    WaitingKey
}

data class SyncRuntimeState(
    val global: GlobalSyncState = GlobalSyncState.Idle,
    val pendingOps: Int = 0,
    val isOnline: Boolean = true,
    val lastSyncAt: Long? = null
)

class SyncStatusStore {
    private val _state = MutableStateFlow(SyncRuntimeState())
    val state: StateFlow<SyncRuntimeState> = _state.asStateFlow()

    fun setGlobal(global: GlobalSyncState) {
        _state.value = _state.value.copy(global = global)
    }

    fun setOnline(isOnline: Boolean) {
        _state.value = _state.value.copy(isOnline = isOnline)
    }

    fun setPending(count: Int) {
        _state.value = _state.value.copy(pendingOps = count)
    }

    fun markSynced() {
        _state.value = _state.value.copy(global = GlobalSyncState.Idle, lastSyncAt = System.currentTimeMillis())
    }
}
