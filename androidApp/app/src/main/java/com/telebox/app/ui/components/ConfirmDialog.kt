package com.telebox.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
            Button(
                onClick = onConfirm,
                colors = if (options.variant == ConfirmVariant.DANGER) {
                    ButtonDefaults.buttonColors(
                        containerColor = scheme.error,
                        contentColor = scheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(options.confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(options.cancelText)
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}
