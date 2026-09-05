package com.telebox.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.telebox.app.data.ConfirmOptions
import com.telebox.app.data.ConfirmVariant

@Composable
fun ConfirmDialog(
    options: ConfirmOptions,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(options.title, style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Text(options.message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    options.confirmText,
                    color = if (options.variant == ConfirmVariant.DANGER) scheme.error else scheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(options.cancelText)
            }
        }
    )
}
