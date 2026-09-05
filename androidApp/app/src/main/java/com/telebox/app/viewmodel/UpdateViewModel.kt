package com.telebox.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telebox.app.data.UpdateState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UpdateViewModel : ViewModel() {
    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            delay(5_000)
            checkForUpdates()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _state.update { it.copy(checking = true, error = null) }
            delay(400)
            _state.update { it.copy(checking = false, available = false) }
        }
    }

    fun downloadAndInstall() {
        viewModelScope.launch {
            _state.update { it.copy(downloading = true, progress = 0) }
            for (pct in 10..100 step 10) {
                delay(180)
                _state.update { it.copy(progress = pct) }
            }
            _state.update { it.copy(downloading = false, available = false) }
        }
    }

    fun dismissUpdate() {
        _state.update { it.copy(available = false) }
    }
}
