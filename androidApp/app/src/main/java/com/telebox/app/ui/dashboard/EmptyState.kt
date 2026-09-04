package com.telebox.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.ui.theme.TeleBoxTheme

@Composable
fun EmptyState(onUpload: () -> Unit) {
    val colors = TeleBoxTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(192.dp)) {
            val w = size.width
            val h = size.height
            drawOval(
                color = colors.primary.copy(alpha = 0.08f),
                topLeft = Offset(w * 0.15f, h * 0.4f),
                size = Size(w * 0.7f, h * 0.4f)
            )
            val folder = Path().apply {
                moveTo(w * 0.2f, h * 0.4f)
                lineTo(w * 0.2f, h * 0.7f)
                lineTo(w * 0.8f, h * 0.7f)
                lineTo(w * 0.8f, h * 0.4f)
                close()
            }
            drawPath(folder, colors.surface)
            drawPath(folder, colors.primary.copy(alpha = 0.3f), style = Stroke(width = 2f))
            drawCircle(
                color = colors.primary.copy(alpha = 0.15f),
                radius = w * 0.1f,
                center = Offset(w * 0.5f, h * 0.55f)
            )
            drawLine(colors.primary, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.6f), strokeWidth = 4f)
            drawLine(colors.primary, Offset(w * 0.45f, h * 0.55f), Offset(w * 0.55f, h * 0.55f), strokeWidth = 4f)
            drawRoundRect(Color(0xFF3B82F6), Offset(w * 0.65f, h * 0.25f), Size(w * 0.12f, h * 0.15f), CornerRadius(8f, 8f))
        }
        Text("This folder is empty", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Drag and drop files here, or click the button below to upload from your computer.",
            color = colors.subtext,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onUpload,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Upload Files", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(24.dp))
        Text("Tip: Use Cmd + F to search", color = colors.subtext.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}
