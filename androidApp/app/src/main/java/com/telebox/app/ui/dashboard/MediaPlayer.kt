package com.telebox.app.ui.dashboard

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.telebox.app.data.TelegramFile
import com.telebox.app.util.isAudioFile
import com.telebox.app.util.isVideoFile
import com.telebox.app.util.mediaPlaybackUri
import com.telebox.app.util.mimeTypeForFile
import kotlinx.coroutines.delay

@kotlin.OptIn(UnstableApi::class)
@Composable
fun MediaPlayerDialog(
    file: TelegramFile,
    currentIndex: Int,
    totalItems: Int,
    streamUrl: String?,
    loading: Boolean,
    error: String?,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val isVideo = isVideoFile(file.name)
    val isAudio = isAudioFile(file.name)
    val source = streamUrl

    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var userSeeking by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                durationMs = player.duration.coerceAtLeast(0L)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playerError = error.message ?: "Playback failed"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(source, file.id) {
        playerError = null
        if (source.isNullOrBlank()) return@LaunchedEffect
        player.stop()
        player.clearMediaItems()
        val mime = mimeTypeForFile(file.name)
        val mediaItem = MediaItem.Builder()
            .setUri(mediaPlaybackUri(source))
            .setMimeType(if (mime == "application/octet-stream") null else mime)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    LaunchedEffect(player, source) {
        while (true) {
            if (!userSeeking) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.coerceAtLeast(0L)
                val dur = durationMs
                sliderPosition = if (dur > 0L) (positionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
            }
            delay(200)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .systemBarsPadding()
    ) {
        FilledTonalIconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = scheme.surface.copy(alpha = 0.28f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Close")
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                loading || source == null && error == null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = scheme.primary,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        "Preparing stream...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                error != null || playerError != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = scheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        playerError ?: error ?: "Unable to play media",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                isVideo -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color.Black
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    this.player = player
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { view -> view.player = player },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                isAudio -> AudioNowPlaying(
                    fileName = file.name,
                    isPlaying = isPlaying,
                    sliderPosition = sliderPosition,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeek = { value ->
                        userSeeking = true
                        sliderPosition = value
                    },
                    onSeekFinished = {
                        val dur = durationMs
                        if (dur > 0L) player.seekTo((sliderPosition * dur).toLong())
                        userSeeking = false
                    },
                    onTogglePlay = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                    onPrev = onPrev,
                    onNext = onNext
                )

                else -> Text("Unsupported media type", color = Color.White)
            }

            if (!isAudio) {
                Spacer(Modifier.height(20.dp))
                Text(
                    file.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (totalItems > 0) {
                        "Streaming from Telegram Drive  •  ${currentIndex + 1}/$totalItems"
                    } else {
                        "Streaming from Telegram Drive"
                    },
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (isVideo) {
                    Slider(
                        value = sliderPosition,
                        onValueChange = {
                            userSeeking = true
                            sliderPosition = it
                        },
                        onValueChangeFinished = {
                            val dur = durationMs
                            if (dur > 0L) player.seekTo((sliderPosition * dur).toLong())
                            userSeeking = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = scheme.primary,
                            activeTrackColor = scheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatClock(positionMs), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Text(formatClock(durationMs), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    }
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(onClick = onPrev, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous")
                        }
                        FilledIconButton(
                            onClick = { if (player.isPlaying) player.pause() else player.play() },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Outlined.SkipNext, contentDescription = "Next")
                        }
                    }
                }
            } else {
                Text(
                    text = if (totalItems > 0) {
                        "Streaming from Telegram Drive  •  ${currentIndex + 1}/$totalItems"
                    } else {
                        "Streaming from Telegram Drive"
                    },
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioNowPlaying(
    fileName: String,
    isPlaying: Boolean,
    sliderPosition: Float,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val spin = rememberInfiniteTransition(label = "disc")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc-angle"
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(scheme.primaryContainer, scheme.primary, scheme.tertiary)
                    )
                )
                .rotate(if (isPlaying) angle else 0f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(168.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Audiotrack,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            fileName,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(20.dp))
        Slider(
            value = sliderPosition,
            onValueChange = onSeek,
            onValueChangeFinished = onSeekFinished,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = scheme.primary,
                activeTrackColor = scheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatClock(positionMs), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            Text(formatClock(durationMs), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledTonalIconButton(
                onClick = onPrev,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(28.dp))
            }
            FilledIconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(76.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }
            FilledTonalIconButton(
                onClick = onNext,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Outlined.SkipNext, contentDescription = "Next", modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(1.dp))
    }
}

private fun formatClock(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = (ms / 1000L).toInt()
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
