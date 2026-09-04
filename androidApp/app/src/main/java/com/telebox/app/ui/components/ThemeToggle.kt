package com.telebox.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.AppThemeMode
import com.telebox.app.ui.theme.TeleBoxTheme

@Composable
fun ThemeToggle(
    theme: AppThemeMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTip by remember { mutableStateOf(false) }
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        IconButton(onClick = {
            showTip = false
            onToggle()
        }) {
            Icon(
                imageVector = if (theme == AppThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = if (theme == AppThemeMode.DARK) "Switch to Light Mode" else "Switch to Dark Mode",
                tint = TeleBoxTheme.colors.subtext,
                modifier = Modifier.size(20.dp)
            )
        }
        if (showTip) {
            Surface(
                modifier = Modifier.padding(top = 40.dp),
                color = TeleBoxTheme.colors.surface,
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = if (theme == AppThemeMode.DARK) "Light Mode" else "Dark Mode",
                    color = TeleBoxTheme.colors.text,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
