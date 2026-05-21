package com.archieapps.fles.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.fles.data.remote.SessionEvents
import com.archieapps.fles.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            SessionEvents.unauthorized.collect {
                authRepository.logout()
                _state.value = SessionState.Unauthenticated
            }
        }
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _state.value = SessionState.Loading
            val token = authRepository.getSavedToken()
            _state.value = if (token.isNullOrBlank()) {
                SessionState.Unauthenticated
            } else {
                runCatching { authRepository.me() }
                    .fold(
                        onSuccess = { SessionState.Authenticated },
                        onFailure = {
                            if (it is HttpException && (it.code() == 401 || it.code() == 403)) {
                                authRepository.logout()
                                SessionState.Unauthenticated
                            } else {
                                SessionState.Authenticated
                            }
                        }
                    )
            }
        }
    }

    fun markAuthenticated() {
        _state.value = SessionState.Authenticated
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = SessionState.Unauthenticated
        }
    }
}

sealed interface SessionState {
    data object Loading : SessionState
    data object Authenticated : SessionState
    data object Unauthenticated : SessionState
}
