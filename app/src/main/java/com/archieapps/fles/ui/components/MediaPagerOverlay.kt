package com.archieapps.fles.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.archieapps.fles.ui.model.FileItem
import com.archieapps.fles.ui.model.FileKind
import com.archieapps.fles.ui.theme.withAlpha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MediaPagerOverlay(
    files: List<FileItem>,
    initialIndex: Int,
    onEnsureLocalFile: suspend (FileItem) -> File,
    onClose: () -> Unit,
    onDownloadCurrent: (suspend (FileItem) -> Unit)? = null
) {
    if (files.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, files.lastIndex),
        pageCount = { files.size }
    )
    var controlsVisible by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(pagerState.currentPage) }
    var activePageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            currentPage = it
            activePageZoomed = false
        }
    }

    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                userScrollEnabled = !activePageZoomed,
                key = { index -> files[index].id }
            ) { page ->
                val file = files[page]
                val isActive = page == currentPage
                MediaPage(
                    file = file,
                    isActive = isActive,
                    controlsVisible = controlsVisible,
                    onToggleControls = { controlsVisible = !controlsVisible },
                    onControlsAutoHide = { controlsVisible = false },
                    onEnsureLocalFile = onEnsureLocalFile,
                    onClose = onClose,
                    onZoomChange = { zoomed -> if (isActive) activePageZoomed = zoomed }
                )
            }

            if (controlsVisible) {
                val currentFile = files.getOrNull(currentPage)
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
                        currentFile?.name.orEmpty(),
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${currentPage + 1}/${files.size}",
                        color = Color.White.withAlpha(0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (onDownloadCurrent != null && currentFile != null) {
                        Spacer(Modifier.width(8.dp))
                        PlayerControlButton(
                            onClick = {
                                if (downloading) return@PlayerControlButton
                                downloading = true
                                scope.launch {
                                    runCatching { onDownloadCurrent(currentFile) }
                                    downloading = false
                                }
                            }
                        ) {
                            if (downloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Filled.Download, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    PlayerControlButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaPage(
    file: FileItem,
    isActive: Boolean,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onControlsAutoHide: () -> Unit,
    onEnsureLocalFile: suspend (FileItem) -> File,
    onClose: () -> Unit,
    onZoomChange: (Boolean) -> Unit
) {
    var localFile by remember(file.id) { mutableStateOf(file.existingLocalFile()) }
    var loadError by remember(file.id) { mutableStateOf<String?>(null) }
    var loading by remember(file.id) { mutableStateOf(localFile == null) }

    LaunchedEffect(file.id) {
        if (localFile != null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        loadError = null
        runCatching { onEnsureLocalFile(file) }
            .onSuccess { localFile = it }
            .onFailure { loadError = it.message ?: "Falha ao carregar arquivo" }
        loading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            loadError != null -> CenteredMessage(loadError!!)
            loading && localFile == null -> CircularProgressIndicator(
                modifier = Modifier.size(48.dp).align(Alignment.Center),
                color = Color.White,
                strokeWidth = 3.dp
            )
            localFile != null && file.kind == FileKind.Video -> {
                if (isActive) {
                    VideoPlayerContent(
                        title = file.name,
                        uri = android.net.Uri.fromFile(localFile).toString(),
                        showTitleBar = false,
                        controlsVisible = controlsVisible,
                        onToggleControls = onToggleControls,
                        onControlsAutoHide = onControlsAutoHide,
                        onClose = onClose
                    )
                } else {
                    VideoThumbnailPreview(
                        file = file,
                        localPath = localFile?.absolutePath,
                        onTap = onToggleControls
                    )
                }
            }
            localFile != null -> ImagePage(
                file = file,
                localFile = localFile!!,
                isActive = isActive,
                onTap = onToggleControls,
                onZoomChange = onZoomChange
            )
            else -> CenteredMessage("Preview indisponível")
        }
    }
}

@Composable
private fun ImagePage(
    file: FileItem,
    localFile: File,
    isActive: Boolean,
    onTap: () -> Unit,
    onZoomChange: (Boolean) -> Unit
) {
    var bitmap by remember(file.id, localFile.absolutePath) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(file.id, localFile.absolutePath) { mutableStateOf(false) }

    LaunchedEffect(file.id, localFile.absolutePath) {
        bitmap = null
        failed = false
        val decoded = runCatching {
            withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(localFile.absolutePath)?.asImageBitmap()
            }
        }.getOrNull()
        if (decoded == null) failed = true else bitmap = decoded
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val current = bitmap
        when {
            current != null -> ZoomableImage(
                bitmap = current,
                contentDescription = file.name,
                isActive = isActive,
                onTap = onTap,
                onZoomChange = onZoomChange
            )
            failed -> CenteredMessage("Não foi possível abrir esta imagem")
            else -> CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun ZoomableImage(
    bitmap: ImageBitmap,
    contentDescription: String?,
    isActive: Boolean,
    onTap: () -> Unit,
    onZoomChange: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isActive) {
        if (!isActive) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            onZoomChange(false)
        }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val previous = scale
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = nextScale
        if (nextScale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
        if ((previous > 1f) != (nextScale > 1f)) onZoomChange(nextScale > 1f)
    }

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }
            .transformable(state = transformState, canPan = { scale > 1f })
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            onZoomChange(false)
                        } else {
                            scale = 2.5f
                            onZoomChange(true)
                        }
                    }
                )
            },
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun VideoThumbnailPreview(file: FileItem, localPath: String?, onTap: () -> Unit) {
    val meta = rememberVideoMeta(localPath)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) },
        contentAlignment = Alignment.Center
    ) {
        val thumb = meta.thumbnail
        if (thumb != null) {
            Image(
                bitmap = thumb,
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.Black.withAlpha(0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun BoxScope.CenteredMessage(message: String) {
    Text(
        message,
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

private fun FileItem.existingLocalFile(): File? {
    if (isSecure) return null
    val path = localPath?.removePrefix("file://") ?: return null
    return File(path).takeIf { it.exists() }
}
