package com.telebox.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telebox.app.data.BandwidthStats
import com.telebox.app.util.formatBytes

@Composable
fun BandwidthWidget(bandwidth: BandwidthStats?) {
    if (bandwidth == null) return
    val scheme = MaterialTheme.colorScheme
    val totalBytes = bandwidth.upBytes + bandwidth.downBytes
    val limit = 250L * 1024 * 1024 * 1024
    val percent = ((totalBytes.toDouble() / limit).toFloat()).coerceIn(0f, 1f)
    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Used today", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier.fillMaxWidth(),
            color = scheme.primary,
            trackColor = scheme.surfaceContainerHighest
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatBytes(totalBytes), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Text("250 GB", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}
