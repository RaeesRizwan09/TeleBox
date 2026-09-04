// file: viewmodel/AppViewModel.kt
package com.telebox.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telebox.app.data.AuthStatus
import com.telebox.app.data.PreferencesStore
import com.telebox.app.data.TelegramRepository
import com.telebox.app.data.ToastMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    application: Application,
    private val repository: TelegramRepository,
    private val store: PreferencesStore
) : AndroidViewModel(application) {

    private val _authStatus = MutableStateFlow(AuthStatus.LOADING)
    val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()

    private val _fatalError = MutableStateFlow<Throwable?>(null)
    val fatalError: StateFlow<Throwable?> = _fatalError.asStateFlow()

    private val _toasts = MutableStateFlow<List<ToastMessage>>(emptyList())
    val toasts: StateFlow<List<ToastMessage>> = _toasts.asStateFlow()

    private var toastSeq = 0L

    init {
        restoreSession()
    }

    /**
     * Attempts to restore a previous Telegram session using stored API credentials.
     * On failure, the app falls back to the unauthenticated state.
     */
    fun restoreSession() {
        viewModelScope.launch {
            _authStatus.value = AuthStatus.LOADING
            try {
                val savedId = store.getApiId()
                if (savedId.isNullOrBlank()) {
                    _authStatus.value = AuthStatus.UNAUTHENTICATED
                    return@launch
                }
                val apiId = savedId.toIntOrNull()
                if (apiId == null) {
                    _authStatus.value = AuthStatus.UNAUTHENTICATED
                    return@launch
                }

                // 1. Initialise the Rust engine (connect)
                repository.connect(apiId)
                // 2. Check if the session is still valid (ping + auto‑reconnect)
                val ok = repository.checkConnection()
                _authStatus.value = if (ok) AuthStatus.AUTHENTICATED else AuthStatus.UNAUTHENTICATED

                if (ok) {
                    Log.i("AppViewModel", "Session restored successfully")
                } else {
                    Log.w("AppViewModel", "Session check failed – need re‑auth")
                }
            } catch (err: Throwable) {
                // If anything goes wrong (network, corrupted session, etc.), clear credentials and show login.
                Log.e("AppViewModel", "Restore failed", err)
                runCatching { store.clearCredentials() }
                _authStatus.value = AuthStatus.UNAUTHENTICATED
                // Optionally show a toast (but avoid spamming on first launch)
                showToast("Session expired. Please sign in again.", isError = true)
            }
        }
    }

    fun onLogin() {
        _authStatus.value = AuthStatus.AUTHENTICATED
        Log.i("AppViewModel", "User logged in")
    }

    fun onLogout() {
        _authStatus.value = AuthStatus.UNAUTHENTICATED
        Log.i("AppViewModel", "User logged out")
    }

    fun reportFatal(error: Throwable) {
        _fatalError.value = error
        Log.e("AppViewModel", "Fatal error reported", error)
    }

    fun clearFatal() {
        _fatalError.value = null
        restoreSession()
    }

    fun showToast(text: String, isError: Boolean = false) {
        toastSeq += 1
        val message = ToastMessage(id = toastSeq, text = text, isError = isError)
        _toasts.update { it + message }
    }

    fun dismissToast(id: Long) {
        _toasts.update { it.filterNot { it.id == id } }
    }
}