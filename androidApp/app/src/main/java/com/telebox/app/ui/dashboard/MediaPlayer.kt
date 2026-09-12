package com.telebox.app.ui.dashboard

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.telebox.app.data.TelegramFile
import com.telebox.app.ui.components.rememberFileTypeInfo
import com.telebox.app.ui.theme.yumaClickable
import com.telebox.app.util.isAudioFile
import com.telebox.app.util.isVideoFile
import com.telebox.app.util.mediaPlaybackUri
import com.telebox.app.util.mimeTypeForFile
import kotlinx.coroutines.delay

private val PLAYBACK_SPEEDS = floatArrayOf(1f, 1.25f, 1.5f, 2f, 0.75f)

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
    val accent = rememberFileTypeInfo(file.name).color
    val source = streamUrl

    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var userSeeking by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var speedIndex by remember { mutableIntStateOf(0) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                durationMs = player.duration.coerceAtLeast(0L)
            }
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
                playerError = e.message ?: "Playback failed"
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
                bufferedMs = player.bufferedPosition.coerceAtLeast(0L)
                val dur = durationMs
                sliderPosition = if (dur > 0L) (positionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
            }
            delay(200)
        }
    }

    val buffering = player.playbackState == Player.STATE_BUFFERING && isPlaying
    val progress = sliderPosition
    val buffered = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val speed = PLAYBACK_SPEEDS[speedIndex]

    val onTogglePlay = { if (player.isPlaying) player.pause() else player.play() }
    val onSeek: (Float) -> Unit = { userSeeking = true; sliderPosition = it }
    val onSeekFinished: (Float) -> Unit = {
        val dur = durationMs
        if (dur > 0L) player.seekTo((it * dur).toLong())
        userSeeking = false
    }
    val onCycleSpeed = {
        speedIndex = (speedIndex + 1) % PLAYBACK_SPEEDS.size
        player.setPlaybackSpeed(PLAYBACK_SPEEDS[speedIndex])
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .systemBarsPadding()
    ) {
        // Subtle tonal depth wash derived from the file's accent colour.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerGlassButton(onClick = onClose, size = 44.dp, iconSize = 22.dp,
                containerColor = scheme.surface.copy(alpha = 0.28f), contentColor = Color.White,
                icon = Icons.Outlined.Close)
            if (totalItems > 1) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(scheme.surface.copy(alpha = 0.28f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("${currentIndex + 1}/$totalItems", color = Color.White,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                loading || (source == null && error == null) -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(48.dp), strokeWidth = 4.dp)
                    Text("Preparing stream...", color = Color.White, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp))
                }
                error != null || playerError != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = scheme.error, modifier = Modifier.size(48.dp))
                    Text(playerError ?: error ?: "Unable to play media", color = Color.White,
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                }
                isVideo -> VideoPlayer(
                    player = player,
                    isPlaying = isPlaying,
                    buffering = buffering,
                    progress = progress,
                    buffered = buffered,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    speed = speed,
                    accent = accent,
                    onGlass = Color.White,
                    onTogglePlay = onTogglePlay,
                    onSeek = onSeek,
                    onSeekFinished = onSeekFinished,
                    onPrev = onPrev,
                    onNext = onNext,
                    onCycleSpeed = onCycleSpeed
                )
                isAudio -> AudioNowPlaying(
                    fileName = file.name,
                    subtitle = "Audio track  •  ${file.sizeStr}",
                    isPlaying = isPlaying,
                    progress = progress,
                    buffered = buffered,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    speed = speed,
                    accent = accent,
                    onGlass = Color.White,
                    onSeek = onSeek,
                    onSeekFinished = onSeekFinished,
                    onTogglePlay = onTogglePlay,
                    onPrev = onPrev,
                    onNext = onNext,
                    onCycleSpeed = onCycleSpeed
                )
                else -> Text("Unsupported media type", color = Color.White)
            }
        }
    }
}

@Composable
private fun AudioNowPlaying(
    fileName: String,
    subtitle: String,
    isPlaying: Boolean,
    progress: Float,
    buffered: Float,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    accent: Color,
    onGlass: Color,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCycleSpeed: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        AudioCover(accent = accent, isPlaying = isPlaying, seed = fileName, modifier = Modifier.size(240.dp))
        Spacer(Modifier.height(28.dp))
        Text(
            fileName,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee()
        )
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(26.dp))
        PlayerSeekBar(progress, buffered, onSeek, onSeekFinished, accent, onGlass, Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatClock(positionMs), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            Text(formatClock(durationMs), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            SpeedChip(speed = speed, onClick = onCycleSpeed)
            PlayerGlassButton(onClick = onPrev, size = 60.dp, iconSize = 30.dp,
                containerColor = Color.White.copy(alpha = 0.14f), contentColor = Color.White,
                icon = Icons.Outlined.SkipPrevious)
            PlayerGlassButton(onClick = onTogglePlay, size = 78.dp, iconSize = 40.dp,
                containerColor = accent, contentColor = Color.Black,
                icon = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow)
            PlayerGlassButton(onClick = onNext, size = 60.dp, iconSize = 30.dp,
                containerColor = Color.White.copy(alpha = 0.14f), contentColor = Color.White,
                icon = Icons.Outlined.SkipNext)
            Spacer(Modifier.width(0.dp))
        }
    }
}

@Composable
private fun VideoPlayer(
    player: Player,
    isPlaying: Boolean,
    buffering: Boolean,
    progress: Float,
    buffered: Float,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    accent: Color,
    onGlass: Color,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCycleSpeed: () -> Unit
) {
    var controls by remember { mutableStateOf(true) }
    LaunchedEffect(isPlaying, controls) {
        if (isPlaying && controls) {
            delay(3200)
            controls = false
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(28.dp))
            .clickable { controls = !controls }
    ) {
        Surface(color = Color.Black, shape = RoundedCornerShape(28.dp)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = player
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                },
                update = { view -> view.player = player },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (buffering) {
            CircularProgressIndicator(
                color = Color.White, modifier = Modifier.align(Alignment.Center).size(46.dp), strokeWidth = 4.dp
            )
        }

        AnimatedVisibility(controls, Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent, 0.75f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
                if (!isPlaying) {
                    PlayerGlassButton(
                        onClick = onTogglePlay, size = 74.dp, iconSize = 38.dp,
                        containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White,
                        icon = Icons.Outlined.PlayArrow,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlayerSeekBar(progress, buffered, onSeek, onSeekFinished, Color.White, onGlass, Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatClock(positionMs), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                        SpeedChip(speed = speed, onClick = onCycleSpeed)
                        Text(formatClock(durationMs), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerGlassButton(onClick = onPrev, size = 58.dp, iconSize = 30.dp,
                            containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White,
                            icon = Icons.Outlined.SkipPrevious)
                        PlayerGlassButton(onClick = onTogglePlay, size = 72.dp, iconSize = 38.dp,
                            containerColor = Color.White, contentColor = Color.Black,
                            icon = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow)
                        PlayerGlassButton(onClick = onNext, size = 58.dp, iconSize = 30.dp,
                            containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White,
                            icon = Icons.Outlined.SkipNext)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioCover(accent: Color, isPlaying: Boolean, seed: String, modifier: Modifier = Modifier) {
    val (c1, c2) = remember(seed) {
        val h = (seed.hashCode().let { if (it < 0) -it else it } % 360)
        Color.hsv(h.toFloat(), 0.62f, 0.95f) to Color.hsv(((h + 38) % 360).toFloat(), 0.72f, 0.66f)
    }
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.White.copy(0.14f), 1f to Color.Transparent)))
        if (isPlaying) {
            EqualizerBars(isPlaying = true, color = Color.White, modifier = Modifier.size(120.dp, 64.dp))
        } else {
            Icon(Icons.Outlined.Audiotrack, contentDescription = null, tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(72.dp))
        }
    }
}

@Composable
private fun EqualizerBars(isPlaying: Boolean, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
        repeat(4) { i ->
            val target by transition.animateFloat(
                initialValue = 8f,
                targetValue = if (isPlaying) (16f + i * 10) else 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 380 + i * 130, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "eq-$i"
            )
            Box(Modifier.width(6.dp).height(target.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun PlayerSeekBar(
    progress: Float,
    buffered: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    accent: Color,
    onGlass: Color,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableFloatStateOf(0f) }
    val shown = if (dragging) dragFrac else progress
    Box(modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(onGlass.copy(alpha = 0.22f))) {
            Box(Modifier.fillMaxWidth(buffered.coerceIn(0f, 1f)).height(5.dp).background(onGlass.copy(alpha = 0.4f)))
            Box(Modifier.fillMaxWidth(shown.coerceIn(0f, 1f)).height(5.dp).background(accent))
        }
        Slider(
            value = shown.coerceIn(0f, 1f),
            onValueChange = { dragging = true; dragFrac = it; onSeek(it) },
            onValueChangeFinished = { dragging = false; onSeekFinished(dragFrac) },
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                disabledThumbColor = accent,
                disabledActiveTrackColor = Color.Transparent,
                disabledInactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().height(28.dp)
        )
    }
}

@Composable
private fun SpeedChip(speed: Float, onClick: () -> Unit) {
    Box(
        Modifier
            .yumaClickable(onClick = onClick, haptic = false)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text("${if (speed % 1f == 0f) speed.toInt() else speed}x", color = Color.White,
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PlayerGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    iconSize: Dp = 30.dp,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector
) {
    Box(
        modifier
            .yumaClickable(onClick = onClick)
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
    }
}

private fun formatClock(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = (ms / 1000L).toInt()
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
