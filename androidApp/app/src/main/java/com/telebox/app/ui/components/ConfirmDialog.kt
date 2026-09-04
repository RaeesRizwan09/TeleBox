package com.telebox.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.ConfirmOptions
import com.telebox.app.data.ConfirmVariant
import com.telebox.app.ui.theme.TeleBoxTheme

@Composable
fun ConfirmDialog(
    options: ConfirmOptions,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(384.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.confirmSurface)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Text(
                text = options.title,
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = options.message,
                color = colors.subtext,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text(options.cancelText, color = colors.subtext, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onConfirm) {
                    val confirmColor = if (options.variant == ConfirmVariant.DANGER) colors.danger else colors.primary
                    Text(
                        options.confirmText,
                        color = confirmColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
