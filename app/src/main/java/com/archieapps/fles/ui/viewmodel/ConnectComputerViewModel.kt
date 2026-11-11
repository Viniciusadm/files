package com.archieapps.fles.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.fles.data.repository.CryptoRepository
import com.archieapps.fles.data.repository.PairingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectComputerUiState(
    val code: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val connectedDevice: String? = null,
    val connected: Boolean = false,
    val fingerprint: String? = null,
    val hasKey: Boolean = true
)

class ConnectComputerViewModel(
    private val pairingRepository: PairingRepository,
    private val cryptoRepository: CryptoRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ConnectComputerUiState())
    val state: StateFlow<ConnectComputerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(hasKey = cryptoRepository.hasKey(), fingerprint = cryptoRepository.fingerprint()) }
        }
    }

    fun onCodeChange(value: String) {
        val clean = value.uppercase().filter { it.isLetterOrDigit() }.take(10)
        val formatted = if (clean.length > 5) "${clean.substring(0, 5)}-${clean.substring(5)}" else clean
        _state.update { it.copy(code = formatted, error = null) }
    }

    fun connect() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { pairingRepository.approve(_state.value.code) }
                .onSuccess { device ->
                    _state.update { it.copy(loading = false, connected = true, connectedDevice = device, code = "") }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.userMessage()) }
                }
        }
    }

    fun reset() {
        _state.update { it.copy(connected = false, connectedDevice = null, code = "", error = null) }
    }
}
