package com.archieapps.fles.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VideoMeta(
    val thumbnail: ImageBitmap?,
    val durationMs: Long
)

private object VideoThumbnailCache {
    private val maxBytes = (Runtime.getRuntime().maxMemory() / 16L).toInt().coerceAtLeast(4 * 1024 * 1024)
    val bitmaps: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    val durations: LruCache<String, Long> = LruCache(256)
}

private suspend fun extractVideoMeta(path: String): Pair<Bitmap?, Long> = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(path)
        val frame = runCatching {
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }.getOrNull()
        val duration = runCatching {
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        }.getOrDefault(0L)
        frame to duration
    } catch (_: Throwable) {
        null to 0L
    } finally {
        runCatching { retriever.release() }
    }
}

@Composable
fun rememberVideoMeta(localPath: String?): VideoMeta {
    var meta by remember(localPath) {
        mutableStateOf(
            VideoMeta(
                thumbnail = localPath?.let { VideoThumbnailCache.bitmaps.get(it)?.asImageBitmap() },
                durationMs = localPath?.let { VideoThumbnailCache.durations.get(it) } ?: 0L
            )
        )
    }
    LaunchedEffect(localPath) {
        val path = localPath ?: return@LaunchedEffect
        if (VideoThumbnailCache.bitmaps.get(path) != null) return@LaunchedEffect
        val (bitmap, duration) = extractVideoMeta(path)
        if (bitmap != null) VideoThumbnailCache.bitmaps.put(path, bitmap)
        VideoThumbnailCache.durations.put(path, duration)
        meta = VideoMeta(bitmap?.asImageBitmap(), duration)
    }
    return meta
}

fun formatVideoDuration(durationMs: Long): String {
    if (durationMs <= 0) return ""
    val totalSeconds = (durationMs / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
