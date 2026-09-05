package com.telebox.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telebox.app.data.AppThemeMode
import com.telebox.app.data.AuthStatus
import com.telebox.app.ui.components.ConfirmDialog
import com.telebox.app.ui.components.ToastHost
import com.telebox.app.ui.components.UpdateBanner
import com.telebox.app.ui.theme.TeleBoxTheme
import com.telebox.app.viewmodel.AppViewModel
import com.telebox.app.viewmodel.AuthViewModel
import com.telebox.app.viewmodel.ConfirmViewModel
import com.telebox.app.viewmodel.DashboardViewModel
import com.telebox.app.viewmodel.TeleBoxViewModelFactory
import com.telebox.app.viewmodel.ThemeViewModel
import com.telebox.app.viewmodel.UpdateViewModel

@Composable
fun TeleBoxRoot() {
    val activity = LocalContext.current as ComponentActivity
    val application = activity.application
    val factory = remember { TeleBoxViewModelFactory(application) }
    val themeViewModel: ThemeViewModel = viewModel(factory = factory)
    val confirmViewModel: ConfirmViewModel = viewModel(factory = factory)
    val updateViewModel: UpdateViewModel = viewModel(factory = factory)
    val appViewModel: AppViewModel = viewModel(factory = factory)
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val dashFactory = remember(appViewModel, confirmViewModel) {
        TeleBoxViewModelFactory(application, appViewModel, confirmViewModel)
    }
    val dashboardViewModel: DashboardViewModel = viewModel(factory = dashFactory)

    val theme by themeViewModel.theme.collectAsStateWithLifecycle()
    val authStatus by appViewModel.authStatus.collectAsStateWithLifecycle()
    val fatal by appViewModel.fatalError.collectAsStateWithLifecycle()
    val toasts by appViewModel.toasts.collectAsStateWithLifecycle()
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    val confirmOptions by confirmViewModel.options.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.state.collectAsStateWithLifecycle()

    TeleBoxTheme(darkTheme = theme == AppThemeMode.DARK) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (fatal != null) {
                    ErrorScreen(error = fatal, onReload = appViewModel::clearFatal)
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        UpdateBanner(
                            state = updateState,
                            onUpdate = updateViewModel::downloadAndInstall,
                            onDismiss = updateViewModel::dismissUpdate
                        )
                        when (authStatus) {
                            AuthStatus.LOADING -> SplashScreen()
                            AuthStatus.AUTHENTICATED -> DashboardScreen(
                                state = dashboardState,
                                viewModel = dashboardViewModel,
                                theme = theme,
                                onToggleTheme = themeViewModel::toggleTheme
                            )
                            AuthStatus.UNAUTHENTICATED -> AuthWizardScreen(
                                state = authState,
                                theme = theme,
                                onToggleTheme = themeViewModel::toggleTheme,
                                onApiIdChange = authViewModel::onApiIdChange,
                                onApiHashChange = authViewModel::onApiHashChange,
                                onPhoneChange = authViewModel::onPhoneChange,
                                onCodeChange = authViewModel::onCodeChange,
                                onPasswordChange = authViewModel::onPasswordChange,
                                onSetupSubmit = authViewModel::submitSetup,
                                onPhoneSubmit = authViewModel::submitPhone,
                                onCodeSubmit = { authViewModel.submitCode(appViewModel::onLogin) },
                                onPasswordSubmit = { authViewModel.submitPassword(appViewModel::onLogin) },
                                onStartQr = authViewModel::startQrLogin,
                                onSwitchPhone = authViewModel::switchToPhone,
                                onRefreshQr = authViewModel::refreshQr,
                                onGoSetup = authViewModel::goToSetup,
                                onGoPhone = authViewModel::goToPhone,
                                onGoCode = authViewModel::goToCode,
                                onShowHelp = authViewModel::setShowHelp,
                                onShowDonate = authViewModel::setShowDonate,
                                onQrPoll = { authViewModel.startQrPolling(appViewModel::onLogin) },
                                onDevLogin = appViewModel::onLogin
                            )
                        }
                    }
                }
                ToastHost(toasts = toasts, onDismiss = appViewModel::dismissToast)
                confirmOptions?.let { options ->
                    ConfirmDialog(
                        options = options,
                        onConfirm = confirmViewModel::onConfirm,
                        onCancel = confirmViewModel::onCancel
                    )
                }
            }
        }
    }
}
