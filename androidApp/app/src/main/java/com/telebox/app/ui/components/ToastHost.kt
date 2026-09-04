package com.telebox.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.ToastMessage
import com.telebox.app.ui.theme.TeleBoxTheme
import kotlinx.coroutines.delay

@Composable
fun ToastHost(
    toasts: List<ToastMessage>,
    onDismiss: (Long) -> Unit
) {
    val colors = TeleBoxTheme.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            toasts.takeLast(3).forEach { toast ->
                LaunchedEffect(toast.id) {
                    delay(3200)
                    onDismiss(toast.id)
                }
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = toast.text,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (toast.isError) colors.danger else colors.surface.copy(alpha = 0.95f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
