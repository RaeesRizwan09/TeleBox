package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.ui.theme.TeleBoxTheme

@Composable
fun DragDropOverlay() {
    val colors = TeleBoxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = colors.primary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Drop files to upload", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Files will be uploaded to the current folder", color = colors.subtext, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun ExternalDropBlocker(onUploadClick: () -> Unit) {
    val colors = TeleBoxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 448.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Upload, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Use the Upload Button", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "To upload files, please use the Upload button in the toolbar.\nDrag-and-drop from Finder is not supported.",
                color = colors.subtext,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onUploadClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Open Upload Dialog", fontWeight = FontWeight.Medium)
            }
        }
    }
}
