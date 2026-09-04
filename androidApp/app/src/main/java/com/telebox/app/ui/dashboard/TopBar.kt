package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.AppThemeMode
import com.telebox.app.data.ViewMode
import com.telebox.app.ui.theme.TeleBoxTheme

@Composable
fun TopBar(
    currentFolderName: String,
    selectedCount: Int,
    viewMode: ViewMode,
    searchTerm: String,
    theme: AppThemeMode,
    onSearchChange: (String) -> Unit,
    onShowMoveModal: () -> Unit,
    onBulkDownload: () -> Unit,
    onBulkDelete: () -> Unit,
    onDownloadFolder: () -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(colors.surface.copy(alpha = 0.8f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Start", color = colors.subtext, fontSize = 14.sp)
            Text(" / ", color = colors.subtext, modifier = Modifier.padding(horizontal = 8.dp))
            Text(currentFolderName, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        OutlinedTextField(
            value = searchTerm,
            onValueChange = onSearchChange,
            placeholder = { Text("Search files...", color = colors.subtext, fontSize = 14.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedContainerColor = colors.hover,
                unfocusedContainerColor = colors.hover
            ),
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 448.dp)
                .padding(horizontal = 16.dp)
                .height(44.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectedCount > 0) {
                Text("$selectedCount Selected", color = colors.subtext, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                TextButton(onClick = onShowMoveModal) {
                    Text("Move to...", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onBulkDownload) {
                    Text("Download Selected", color = colors.text, fontSize = 12.sp)
                }
                TextButton(onClick = onBulkDelete) {
                    Text("Delete", color = colors.danger, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onDownloadFolder) {
                Icon(Icons.Outlined.Storage, contentDescription = "Download All Files", tint = colors.subtext, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onToggleViewMode) {
                Icon(Icons.Outlined.GridView, contentDescription = "Toggle Layout", tint = colors.subtext, modifier = Modifier.size(20.dp))
            }
            BoxDivider()
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (theme == AppThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = colors.subtext,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxDivider() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(TeleBoxTheme.colors.border)
    )
}
