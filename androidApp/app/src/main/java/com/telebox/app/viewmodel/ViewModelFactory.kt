package com.telebox.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telebox.app.TeleBoxApplication

class TeleBoxViewModelFactory(
    private val application: Application,
    private val appViewModel: AppViewModel? = null,
    private val confirmViewModel: ConfirmViewModel? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = application as TeleBoxApplication
        return when {
            modelClass.isAssignableFrom(ThemeViewModel::class.java) -> ThemeViewModel(application) as T
            modelClass.isAssignableFrom(ConfirmViewModel::class.java) -> ConfirmViewModel() as T
            modelClass.isAssignableFrom(UpdateViewModel::class.java) -> UpdateViewModel() as T
            modelClass.isAssignableFrom(AppViewModel::class.java) ->
                AppViewModel(application, app.repository, app.preferencesStore) as T
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(application, app.repository, app.preferencesStore) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(
                    application,
                    app.repository,
                    app.preferencesStore,
                    confirmViewModel ?: ConfirmViewModel(),
                    appViewModel ?: AppViewModel(application, app.repository, app.preferencesStore)
                ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
        }
    }
}
