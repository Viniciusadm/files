package com.archieapps.fles.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SessionEvents {
    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorized = _unauthorized.asSharedFlow()

    fun emitUnauthorized() {
        _unauthorized.tryEmit(Unit)
    }
}
