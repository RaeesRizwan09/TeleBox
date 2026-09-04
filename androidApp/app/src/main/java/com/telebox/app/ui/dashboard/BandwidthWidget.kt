package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.BandwidthStats
import com.telebox.app.ui.theme.TeleBoxTheme
import com.telebox.app.util.formatBytes

@Composable
fun BandwidthWidget(bandwidth: BandwidthStats?) {
    if (bandwidth == null) return
    val colors = TeleBoxTheme.colors
    val totalBytes = bandwidth.upBytes + bandwidth.downBytes
    val limit = 250L * 1024 * 1024 * 1024
    val percent = ((totalBytes.toDouble() / limit) * 100.0).coerceAtMost(100.0).toFloat()
    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Used Today:", color = colors.subtext, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(colors.border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent / 100f)
                    .clip(RoundedCornerShape(99.dp))
                    .background(colors.primary)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatBytes(totalBytes), color = colors.subtext.copy(alpha = 0.7f), fontSize = 10.sp)
            Text("250 GB", color = colors.subtext.copy(alpha = 0.7f), fontSize = 10.sp)
        }
    }
}
