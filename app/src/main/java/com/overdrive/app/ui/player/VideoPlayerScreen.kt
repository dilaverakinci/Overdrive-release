package com.overdrive.app.ui.player

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.overdrive.app.R
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.ui.view.ZoomableVideoView
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun VideoPlayerScreen(
    videoPath: String,
    videoTitle: String,
    playlistPaths: List<String> = emptyList(),
    playlistTitles: List<String> = emptyList(),
    initialPlaylistIndex: Int = 0,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(initialPlaylistIndex) }
    val currentPath = remember(currentIndex, videoPath, playlistPaths) {
        if (playlistPaths.isNotEmpty() && currentIndex in playlistPaths.indices) {
            playlistPaths[currentIndex]
        } else {
            videoPath
        }
    }
    val currentTitle = remember(currentIndex, videoTitle, playlistTitles) {
        if (playlistTitles.isNotEmpty() && currentIndex in playlistTitles.indices) {
            playlistTitles[currentIndex]
        } else {
            videoTitle.ifEmpty { File(currentPath).name }
        }
    }

    var videoViewRef by remember { mutableStateOf<ZoomableVideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }
    var selectedQuadrant by remember { mutableStateOf(ZoomableVideoView.Quadrant.ALL) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(controlsVisible, isPlaying, lastInteractionTime) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Playhead polling loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewRef?.let { vv ->
                currentPosMs = vv.currentPosition.toLong()
                val d = vv.duration.toLong()
                if (d > 0) durationMs = d
            }
            delay(250)
        }
    }

    DisposableEffect(currentPath) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    val uri = remember(currentPath) {
        when {
            currentPath.startsWith("http://") || currentPath.startsWith("https://") -> Uri.parse(currentPath)
            currentPath.startsWith("content://") -> Uri.parse(currentPath)
            else -> Uri.fromFile(File(currentPath))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    controlsVisible = !controlsVisible
                    if (controlsVisible) {
                        lastInteractionTime = System.currentTimeMillis()
                    }
                }
            )
    ) {
        // 1. Hardware Video Texture
        AndroidView(
            factory = { context ->
                ZoomableVideoView(context).apply {
                    videoViewRef = this
                    if (currentPath.contains("dvr_") || currentPath.contains("dashcam")) {
                        setLayout(ZoomableVideoView.Layout.DASHCAM)
                    } else {
                        setLayout(ZoomableVideoView.Layout.STANDARD)
                    }
                    setVideoURI(uri)
                    setQuadrant(selectedQuadrant, false)

                    // Single tap toggles chrome controls
                    setOnClickListener {
                        controlsVisible = !controlsVisible
                        if (controlsVisible) {
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    }

                    // Double tap zooms into quadrant or resets to full mosaic
                    setOnDoubleTapListener { target ->
                        selectedQuadrant = target
                        setQuadrant(target, true)
                        controlsVisible = true
                        lastInteractionTime = System.currentTimeMillis()
                    }

                    setOnPreparedListener { mp ->
                        mediaPlayerRef = mp
                        isBuffering = false
                        durationMs = duration.toLong()
                        mp.isLooping = false
                        setPlaybackVolume(if (isMuted) 0f else 1f)
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                mp.playbackParams = mp.playbackParams.setSpeed(selectedSpeed)
                            }
                        } catch (e: Exception) {
                            // Ignored if speed change not supported on platform
                        }
                        start()
                        isPlaying = true
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        currentPosMs = durationMs
                        controlsVisible = true
                        // Auto play next clip if in playlist
                        if (playlistPaths.isNotEmpty() && currentIndex < playlistPaths.size - 1) {
                            currentIndex++
                        }
                    }
                }
            },
            update = { vv ->
                videoViewRef = vv
                if (vv.getQuadrant() != selectedQuadrant) {
                    vv.setQuadrant(selectedQuadrant, true)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = OverdriveTheme.colors.primary,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        // 2. Animated Overlay Controls (Top & Bottom gradient bars)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient Scrim & Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { lastInteractionTime = System.currentTimeMillis() }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = currentTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatTimestampFromFilename(currentTitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Right Top Controls: Speed & Mute
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Speed Selector
                            Box {
                                Surface(
                                    onClick = { showSpeedMenu = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${selectedSpeed}x",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
                                    listOf(0.5f, 1.0f, 1.5f, 2.0f, 4.0f).forEach { speed ->
                                        DropdownMenuItem(
                                            text = { Text("${speed}x") },
                                            onClick = {
                                                selectedSpeed = speed
                                                try {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                        mediaPlayerRef?.let { mp ->
                                                            val params = mp.playbackParams
                                                            params.setSpeed(speed)
                                                            mp.playbackParams = params
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    // ignore
                                                }
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Mute Toggle
                            Surface(
                                onClick = {
                                    isMuted = !isMuted
                                    videoViewRef?.setPlaybackVolume(if (isMuted) 0f else 1f)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Mute Toggle",
                                        tint = if (isMuted) OverdriveTheme.colors.statusWarning else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Play/Pause Overlay Action
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(
                        onClick = {
                            videoViewRef?.let { vv ->
                                if (isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                        },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(2.dp, OverdriveTheme.colors.primary),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // Bottom Gradient Scrim & Transport Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { lastInteractionTime = System.currentTimeMillis() }
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Quadrant Camera Selector (2x2 Grid Affordance)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VideoQuadrantSelector(
                                selectedQuadrant = selectedQuadrant,
                                onQuadrantSelected = { quad ->
                                    selectedQuadrant = quad
                                    videoViewRef?.setQuadrant(quad, true)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )
                        }

                        // 2. Timeline Scrubber & Times
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = formatDuration(currentPosMs),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )

                            Slider(
                                value = if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                                onValueChange = { ratio ->
                                    val target = (ratio * durationMs).toLong()
                                    currentPosMs = target
                                    videoViewRef?.seekTo(target.toInt())
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = OverdriveTheme.colors.primary,
                                    activeTrackColor = OverdriveTheme.colors.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = formatDuration(durationMs),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // 3. Playback Transport Actions (Prev, -10s, Play/Pause, +10s, Next)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Clip in Playlist
                            if (playlistPaths.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        if (currentIndex > 0) currentIndex--
                                    },
                                    enabled = currentIndex > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous Clip",
                                        tint = if (currentIndex > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // -10s Replay
                            IconButton(onClick = {
                                val target = (currentPosMs - 10000L).coerceAtLeast(0L)
                                currentPosMs = target
                                videoViewRef?.seekTo(target.toInt())
                            }) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Main Play/Pause
                            Surface(
                                onClick = {
                                    videoViewRef?.let { vv ->
                                        if (isPlaying) {
                                            vv.pause()
                                            isPlaying = false
                                        } else {
                                            vv.start()
                                            isPlaying = true
                                        }
                                    }
                                },
                                shape = CircleShape,
                                color = OverdriveTheme.colors.primary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = OverdriveTheme.colors.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // +10s Fast Forward
                            IconButton(onClick = {
                                val target = (currentPosMs + 10000L).coerceAtMost(durationMs)
                                currentPosMs = target
                                videoViewRef?.seekTo(target.toInt())
                            }) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Next Clip in Playlist
                            if (playlistPaths.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        if (currentIndex < playlistPaths.size - 1) currentIndex++
                                    },
                                    enabled = currentIndex < playlistPaths.size - 1
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next Clip",
                                        tint = if (currentIndex < playlistPaths.size - 1) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

private fun formatTimestampFromFilename(filename: String): String {
    // Filenames like 2026-09-30_14-22-10_f.mp4
    val parts = filename.split("_", ".")
    return if (parts.size >= 2) {
        val date = parts[0]
        val time = parts[1].replace("-", ":")
        "$date $time"
    } else {
        filename
    }
}
