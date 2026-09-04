package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.ui.theme.TeleBoxTheme

@Composable
fun SidebarItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val colors = TeleBoxTheme.colors
    val bg = if (active) colors.primary.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent
    val tint = if (active) colors.primary else colors.subtext
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            color = if (active) colors.primary else colors.subtext,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Delete folder",
                    tint = colors.danger,
                    modifier = Modifier
                        .size(12.dp)
                        .rotate(45f)
                )
            }
        }
    }
}
