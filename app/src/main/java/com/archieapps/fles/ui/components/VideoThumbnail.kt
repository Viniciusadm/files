package com.archieapps.fles.ui.components

import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// O frame de thumbnail do vídeo agora vem do Coil (VideoFrameDecoder), que cuida do
// downsampling e do cache. Aqui ficamos só com a duração, que o Coil não fornece — é
// uma leitura barata de metadados, mantida em cache para não repetir ao rolar.
private val durationCache = LruCache<String, Long>(512)

private suspend fun extractDuration(path: String): Long = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (_: Throwable) {
        0L
    } finally {
        runCatching { retriever.release() }
    }
}

@Composable
fun rememberVideoDuration(localPath: String?): Long {
    var duration by remember(localPath) {
        mutableLongStateOf(localPath?.let { durationCache.get(it) } ?: 0L)
    }
    LaunchedEffect(localPath) {
        val path = localPath ?: return@LaunchedEffect
        durationCache.get(path)?.let { duration = it; return@LaunchedEffect }
        val value = extractDuration(path)
        durationCache.put(path, value)
        duration = value
    }
    return duration
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
