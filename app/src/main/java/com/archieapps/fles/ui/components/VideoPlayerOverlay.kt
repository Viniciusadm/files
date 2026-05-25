package com.archieapps.fles.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha
import kotlinx.coroutines.delay

private val SPEED_STEPS = listOf(1f, 1.5f, 2f, 0.5f)

@Composable
fun VideoPlayerOverlay(
    title: String,
    uri: String,
    onClose: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            VideoPlayerContent(
                title = title,
                uri = uri,
                showTitleBar = true,
                controlsVisible = controlsVisible,
                onToggleControls = { controlsVisible = !controlsVisible },
                onControlsAutoHide = { controlsVisible = false },
                onClose = onClose
            )
        }
    }
}

@Composable
fun VideoPlayerContent(
    title: String,
    uri: String,
    showTitleBar: Boolean,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onControlsAutoHide: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var speedIndex by remember { mutableStateOf(0) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                    durationMs = player.duration.takeIf { it > 0 } ?: 0L
                }
            }
        }
        player.addListener(listener)
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!scrubbing && isPlaying) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
            }
            if (durationMs <= 0L) {
                durationMs = player.duration.takeIf { it > 0 } ?: 0L
            }
            delay(200)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, scrubbing) {
        if (controlsVisible && isPlaying && !scrubbing) {
            delay(3500)
            onControlsAutoHide()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleControls() })
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.player = player
                }
            }
        )

        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp).align(Alignment.Center),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }

        if (controlsVisible) {
            if (showTitleBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.withAlpha(0.4f))
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    PlayerControlButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.withAlpha(0.45f))
                    .pointerInput(player) {
                        detectTapGestures(onTap = {
                            if (player.playbackState == Player.STATE_ENDED) {
                                player.seekTo(0)
                                player.play()
                            } else if (player.isPlaying) player.pause() else player.play()
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.withAlpha(0.45f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SeekBar(
                    positionMs = if (scrubbing) scrubPositionMs else positionMs,
                    durationMs = durationMs,
                    onScrubStart = {
                        scrubbing = true
                        scrubPositionMs = positionMs
                        player.pause()
                    },
                    onScrubChange = { newPos -> scrubPositionMs = newPos },
                    onScrubEnd = { finalPos ->
                        player.seekTo(finalPos)
                        positionMs = finalPos
                        scrubbing = false
                        player.play()
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatVideoDuration(if (scrubbing) scrubPositionMs else positionMs),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    SpeedChip(
                        speed = SPEED_STEPS[speedIndex],
                        onClick = {
                            speedIndex = (speedIndex + 1) % SPEED_STEPS.size
                            player.playbackParameters = PlaybackParameters(SPEED_STEPS[speedIndex])
                        }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        formatVideoDuration(durationMs),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlayerControlButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.withAlpha(0.12f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun SpeedChip(speed: Float, onClick: () -> Unit) {
    val label = if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(FlesTheme.radius.full))
            .background(Color.White.withAlpha(0.18f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onScrubStart: () -> Unit,
    onScrubChange: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit
) {
    var widthPx by remember { mutableStateOf(0) }
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress by remember(positionMs, safeDuration) {
        derivedStateOf { (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) }
    }
    var dragPosition by remember { mutableLongStateOf(positionMs) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(durationMs) {
                if (durationMs <= 0L) return@pointerInput
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val ratio = (startOffset.x / widthPx.toFloat()).coerceIn(0f, 1f)
                        dragPosition = (ratio * durationMs).toLong()
                        onScrubStart()
                        onScrubChange(dragPosition)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val ratio = (change.position.x / widthPx.toFloat()).coerceIn(0f, 1f)
                        dragPosition = (ratio * durationMs).toLong()
                        onScrubChange(dragPosition)
                    },
                    onDragEnd = { onScrubEnd(dragPosition) },
                    onDragCancel = { onScrubEnd(dragPosition) }
                )
            }
            .pointerInput(durationMs) {
                if (durationMs <= 0L) return@pointerInput
                detectTapGestures(onTap = { offset ->
                    val ratio = (offset.x / widthPx.toFloat()).coerceIn(0f, 1f)
                    val target = (ratio * durationMs).toLong()
                    onScrubStart()
                    onScrubChange(target)
                    onScrubEnd(target)
                })
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(FlesTheme.radius.full))
                .background(Color.White.withAlpha(0.3f))
                .align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(3.dp)
                .clip(RoundedCornerShape(FlesTheme.radius.full))
                .background(Color.White)
                .align(Alignment.CenterStart)
        )
        val thumbOffsetPx = (progress * widthPx).toInt()
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offsetPx(thumbOffsetPx)
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

private fun Modifier.offsetPx(x: Int): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(x - placeable.width / 2, 0)
        }
    }
)
