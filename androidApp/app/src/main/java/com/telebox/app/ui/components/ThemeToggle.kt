package com.telebox.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.telebox.app.data.AppThemeMode

@Composable
fun ThemeToggle(
    theme: AppThemeMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (theme) {
        AppThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
        AppThemeMode.LIGHT -> Icons.Outlined.LightMode
        AppThemeMode.DARK -> Icons.Outlined.DarkMode
    }
    val description = when (theme) {
        AppThemeMode.SYSTEM -> "Using system theme. Switch to light"
        AppThemeMode.LIGHT -> "Using light theme. Switch to dark"
        AppThemeMode.DARK -> "Using dark theme. Switch to system"
    }
    FilledTonalIconButton(onClick = onToggle, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = description)
    }
}
