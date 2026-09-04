package com.telebox.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telebox.app.data.AppThemeMode
import com.telebox.app.data.PreferencesStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PreferencesStore(application)

    val theme: StateFlow<AppThemeMode> = store.themeFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppThemeMode.DARK
    )

    fun toggleTheme() {
        viewModelScope.launch {
            val next = if (theme.value == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
            store.saveTheme(next)
        }
    }

    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch { store.saveTheme(mode) }
    }
}
