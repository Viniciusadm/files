package com.archieapps.fles.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.fles.data.remote.toFriendlyMessage
import com.archieapps.fles.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun login(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.email.trim().isEmpty() || current.password.isEmpty()) {
            _state.update { it.copy(error = "Informe email e senha") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                authRepository.login(current.email.trim(), current.password)
            }.onSuccess {
                _state.update { it.copy(loading = false, password = "") }
                onSuccess()
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

fun Throwable.userMessage(): String {
    return toFriendlyMessage()
}
