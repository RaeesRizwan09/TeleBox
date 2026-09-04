package com.telebox.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telebox.app.data.AuthStatus
import com.telebox.app.data.PreferencesStore
import com.telebox.app.data.TelegramRepository
import com.telebox.app.data.ToastMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                repository.connect(apiId)
                val ok = repository.checkConnection()
                _authStatus.value = if (ok) AuthStatus.AUTHENTICATED else AuthStatus.UNAUTHENTICATED
            } catch (err: Throwable) {
                runCatching { store.clearCredentials() }
                _authStatus.value = AuthStatus.UNAUTHENTICATED
            }
        }
    }

    fun onLogin() {
        _authStatus.value = AuthStatus.AUTHENTICATED
    }

    fun onLogout() {
        _authStatus.value = AuthStatus.UNAUTHENTICATED
    }

    fun reportFatal(error: Throwable) {
        _fatalError.value = error
    }

    fun clearFatal() {
        _fatalError.value = null
        restoreSession()
    }

    fun showToast(text: String, isError: Boolean = false) {
        toastSeq += 1
        val message = ToastMessage(id = toastSeq, text = text, isError = isError)
        _toasts.value = _toasts.value + message
    }

    fun dismissToast(id: Long) {
        _toasts.value = _toasts.value.filterNot { it.id == id }
    }
}
