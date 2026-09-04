package com.telebox.app.viewmodel

import androidx.lifecycle.ViewModel
import com.telebox.app.data.ConfirmOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ConfirmViewModel : ViewModel() {
    private val _options = MutableStateFlow<ConfirmOptions?>(null)
    val options: StateFlow<ConfirmOptions?> = _options.asStateFlow()

    private var continuation: Continuation<Boolean>? = null

    suspend fun confirm(options: ConfirmOptions): Boolean {
        continuation?.resume(false)
        _options.value = options
        return suspendCancellableCoroutine { cont ->
            continuation = cont
            cont.invokeOnCancellation {
                _options.value = null
                continuation = null
            }
        }
    }

    fun onConfirm() {
        _options.value = null
        continuation?.resume(true)
        continuation = null
    }

    fun onCancel() {
        _options.value = null
        continuation?.resume(false)
        continuation = null
    }
}
