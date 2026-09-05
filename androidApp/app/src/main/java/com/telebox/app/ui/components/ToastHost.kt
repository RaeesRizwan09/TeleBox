package com.telebox.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telebox.app.data.ToastMessage
import kotlinx.coroutines.delay

@Composable
fun ToastHost(
    toasts: List<ToastMessage>,
    onDismiss: (Long) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.padding(bottom = 88.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            toasts.takeLast(3).forEach { toast ->
                LaunchedEffect(toast.id) {
                    delay(3200)
                    onDismiss(toast.id)
                }
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (toast.isError) scheme.errorContainer else scheme.inverseSurface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.widthIn(max = 420.dp)
                    ) {
                        Text(
                            text = toast.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (toast.isError) scheme.onErrorContainer else scheme.inverseOnSurface,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
