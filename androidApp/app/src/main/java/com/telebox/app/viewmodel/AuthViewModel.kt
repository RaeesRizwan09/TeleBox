package com.telebox.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telebox.app.data.AuthStep
import com.telebox.app.data.LoginMethod
import com.telebox.app.data.PreferencesStore
import com.telebox.app.data.TelegramRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AuthUiState(
    val step: AuthStep = AuthStep.SETUP,
    val loading: Boolean = false,
    val apiId: String = "",
    val apiHash: String = "",
    val phone: String = "",
    val code: String = "",
    val password: String = "",
    val error: String? = null,
    val floodWait: Int? = null,
    val showHelp: Boolean = false,
    val showDonate: Boolean = false,
    val loginMethod: LoginMethod = LoginMethod.PHONE,
    val qrUrl: String? = null,
    val qrPolling: Boolean = false
)

class AuthViewModel(
    application: Application,
    private val repository: TelegramRepository,
    private val store: PreferencesStore
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private var floodJob: Job? = null
    private var qrJob: Job? = null

    init {
        viewModelScope.launch {
            val savedId = store.getApiId()
            val savedHash = store.getApiHash()
            if (!savedId.isNullOrBlank() && !savedHash.isNullOrBlank()) {
                _state.update { it.copy(apiId = savedId, apiHash = savedHash) }
                Log.d("AuthViewModel", "Loaded saved credentials")
            }
        }
    }

    // ─── UI input handlers ──────────────────────────────────────────────────────
    fun onApiIdChange(value: String) = _state.update { it.copy(apiId = value, error = null) }
    fun onApiHashChange(value: String) = _state.update { it.copy(apiHash = value, error = null) }
    fun onPhoneChange(value: String) = _state.update { it.copy(phone = value, error = null) }
    fun onCodeChange(value: String) = _state.update { it.copy(code = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun setShowHelp(show: Boolean) = _state.update { it.copy(showHelp = show) }
    fun setShowDonate(show: Boolean) = _state.update { it.copy(showDonate = show) }

    // ─── Setup step ──────────────────────────────────────────────────────────────
    fun submitSetup() {
        val current = _state.value
        if (current.apiId.contains(' ') || current.apiHash.contains(' ')) {
            _state.update { it.copy(error = "API ID and API Hash cannot contain spaces. Please remove any spaces.") }
            return
        }
        if (current.apiId.isBlank() || current.apiHash.isBlank()) {
            _state.update { it.copy(error = "Both API ID and Hash are required.") }
            return
        }
        viewModelScope.launch {
            store.saveCredentials(current.apiId, current.apiHash)
            _state.update {
                it.copy(
                    error = null,
                    step = AuthStep.PHONE,
                    loginMethod = LoginMethod.PHONE,
                    qrUrl = null,
                    qrPolling = false
                )
            }
            Log.i("AuthViewModel", "Credentials saved, moving to phone step")
        }
    }

    // ─── Phone step ──────────────────────────────────────────────────────────────
    fun submitPhone() {
        viewModelScope.launch {
            val current = _state.value
            _state.update { it.copy(loading = true, error = null) }
            try {
                val idInt = current.apiId.toIntOrNull() ?: error("API ID must be a number")
                // The repository method will initialise the engine and request the code.
                repository.requestAuthCode(current.phone, idInt, current.apiHash)
                _state.update { it.copy(loading = false, step = AuthStep.CODE) }
                Log.d("AuthViewModel", "Code requested successfully")
            } catch (err: Throwable) {
                val msg = err.message ?: err.toString()
                val flood = parseFloodWait(msg)
                if (flood != null) {
                    Log.w("AuthViewModel", "Flood wait detected: $flood seconds")
                    startFloodWait(flood)
                    _state.update { it.copy(loading = false) }
                } else {
                    Log.e("AuthViewModel", "Phone submission failed", err)
                    _state.update { it.copy(loading = false, error = msg) }
                }
            }
        }
    }

    // ─── Code step ───────────────────────────────────────────────────────────────
    fun submitCode(onLogin: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val result = repository.signIn(_state.value.code)
                when {
                    result.success -> {
                        Log.i("AuthViewModel", "Sign‑in successful")
                        onLogin()
                    }
                    result.nextStep == "password" -> {
                        Log.d("AuthViewModel", "2FA required")
                        _state.update { it.copy(loading = false, step = AuthStep.PASSWORD) }
                    }
                    else -> {
                        _state.update { it.copy(loading = false, error = "Unknown error during sign‑in") }
                    }
                }
            } catch (err: Throwable) {
                Log.e("AuthViewModel", "Code verification failed", err)
                _state.update { it.copy(loading = false, error = err.message ?: err.toString()) }
            }
        }
    }

    // ─── Password step ──────────────────────────────────────────────────────────
    fun submitPassword(onLogin: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val result = repository.checkPassword(_state.value.password)
                if (result.success) {
                    Log.i("AuthViewModel", "2FA verified")
                    onLogin()
                } else {
                    _state.update { it.copy(loading = false, error = "Password verification failed.") }
                }
            } catch (err: Throwable) {
                Log.e("AuthViewModel", "Password check failed", err)
                _state.update { it.copy(loading = false, error = err.message ?: err.toString()) }
            }
        }
    }

    // ─── QR login flow ─────────────────────────────────────────────────────────
    fun startQrLogin() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loginMethod = LoginMethod.QR,
                    error = null,
                    loading = true,
                    qrUrl = null
                )
            }
            try {
                val idInt = _state.value.apiId.toIntOrNull() ?: error("API ID must be a number")
                val url = repository.qrLogin(idInt, _state.value.apiHash)
                if (url == "__authorized__") {
                    Log.i("AuthViewModel", "QR login: already authorized")
                    _state.update { it.copy(loading = false) }
                    return@launch
                }
                _state.update { it.copy(loading = false, qrUrl = url, qrPolling = true) }
                Log.d("AuthViewModel", "QR URL generated, starting polling")
                startQrPolling()
            } catch (err: Throwable) {
                Log.e("AuthViewModel", "QR login initiation failed", err)
                _state.update { it.copy(loading = false, error = err.message ?: err.toString()) }
            }
        }
    }

    fun startQrPolling(onLogin: () -> Unit = {}) {
        qrJob?.cancel()
        qrJob = viewModelScope.launch {
            while (isActive && _state.value.qrPolling) {
                delay(3_000)
                try {
                    val result = repository.qrPoll()
                    if (result.success) {
                        Log.i("AuthViewModel", "QR login accepted")
                        _state.update { it.copy(qrPolling = false) }
                        if (result.nextStep == "password") {
                            _state.update { it.copy(step = AuthStep.PASSWORD) }
                        } else {
                            onLogin()
                        }
                        break
                    }
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "QR poll error, continuing", e)
                }
            }
        }
    }

    fun switchToPhone() {
        qrJob?.cancel()
        _state.update {
            it.copy(
                loginMethod = LoginMethod.PHONE,
                qrUrl = null,
                qrPolling = false,
                error = null
            )
        }
        Log.d("AuthViewModel", "Switched to phone login")
    }

    fun goToSetup() {
        qrJob?.cancel()
        _state.update {
            it.copy(
                step = AuthStep.SETUP,
                qrPolling = false,
                qrUrl = null,
                error = null
            )
        }
        Log.d("AuthViewModel", "Navigated back to setup")
    }

    fun goToPhone() {
        _state.update { it.copy(step = AuthStep.PHONE, error = null) }
    }

    fun goToCode() {
        _state.update { it.copy(step = AuthStep.CODE, password = "", error = null) }
    }

    fun refreshQr() {
        startQrLogin()
    }

    // ─── Flood wait handling ────────────────────────────────────────────────────
    private fun startFloodWait(seconds: Int) {
        floodJob?.cancel()
        _state.update { it.copy(floodWait = seconds, error = null) }
        floodJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1
                _state.update { it.copy(floodWait = if (remaining <= 0) null else remaining) }
            }
            Log.d("AuthViewModel", "Flood wait ended")
        }
    }

    private fun parseFloodWait(msg: String): Int? {
        if (!msg.contains("FLOOD_WAIT_")) return null
        val seconds = msg.substringAfter("FLOOD_WAIT_").takeWhile { it.isDigit() }.toIntOrNull()
        return seconds
    }

    override fun onCleared() {
        floodJob?.cancel()
        qrJob?.cancel()
        super.onCleared()
    }
}