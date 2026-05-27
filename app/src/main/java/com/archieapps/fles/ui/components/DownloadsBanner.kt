package com.archieapps.fles.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.archieapps.fles.data.repository.DownloadCenter
import com.archieapps.fles.data.repository.DownloadItem
import com.archieapps.fles.data.repository.DownloadStatus
import com.archieapps.fles.ui.theme.FlesTheme
import androidx.compose.runtime.collectAsState
import kotlin.math.roundToInt

/** Disponibiliza o DownloadCenter para a faixa de downloads sem prop drilling. */
val LocalDownloadCenter = staticCompositionLocalOf<DownloadCenter?> { null }

/**
 * Faixa fixa que aparece sozinha enquanto há downloads. Mostra o progresso agregado
 * e expande para a lista completa, com repetir/dispensar por item.
 */
@Composable
fun DownloadsBanner(modifier: Modifier = Modifier) {
    val center = LocalDownloadCenter.current ?: return
    val items by center.downloads.collectAsState()
    if (items.isEmpty()) return

    val colors = FlesTheme.colors
    var expanded by remember { mutableStateOf(false) }

    val active = items.filter { it.status == DownloadStatus.Downloading }
    val failed = items.count { it.status == DownloadStatus.Failed }
    val knownFractions = active.mapNotNull { it.fraction }
    val aggregate = if (knownFractions.isNotEmpty()) knownFractions.average().toFloat() else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FlesTheme.radius.lg))
            .background(colors.card)
    ) {
        // Cabeçalho (resumo) — toca para expandir/recolher.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures(onTap = { expanded = !expanded }) }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Download, null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        active.isNotEmpty() -> "Baixando ${active.size} ${itemWord(active.size)}"
                        failed > 0 -> "$failed download(s) com falha"
                        else -> "Downloads concluídos"
                    },
                    color = colors.cardForeground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (active.isNotEmpty()) {
                    Spacer(Modifier.size(6.dp))
                    if (aggregate != null) {
                        LinearProgressIndicator(
                            progress = { aggregate },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(FlesTheme.radius.full)),
                            color = colors.primary,
                            trackColor = colors.muted
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(FlesTheme.radius.full)),
                            color = colors.primary,
                            trackColor = colors.muted
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            if (active.isNotEmpty() && aggregate != null) {
                Text(
                    "${(aggregate * 100).roundToInt()}%",
                    color = colors.mutedForeground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                null,
                tint = colors.mutedForeground,
                modifier = Modifier.size(22.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { item ->
                    DownloadRow(
                        item = item,
                        onRetry = { center.retry(item.attachmentId) },
                        onDismiss = { center.dismiss(item.attachmentId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadItem, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val colors = FlesTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.file.name.ifBlank { "Arquivo" },
                color = colors.cardForeground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.size(4.dp))
            when (item.status) {
                DownloadStatus.Downloading -> {
                    val frac = item.fraction
                    if (frac != null) {
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(FlesTheme.radius.full)),
                            color = colors.primary,
                            trackColor = colors.muted
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(FlesTheme.radius.full)),
                            color = colors.primary,
                            trackColor = colors.muted
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    Text(progressLabel(item), color = colors.mutedForeground, fontSize = 11.sp)
                }
                DownloadStatus.Completed -> Text("Concluído", color = colors.mutedForeground, fontSize = 11.sp)
                DownloadStatus.Failed -> Text(item.error ?: "Falha ao baixar", color = colors.destructive, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        when (item.status) {
            DownloadStatus.Completed -> Icon(Icons.Filled.CheckCircle, null, tint = colors.primary, modifier = Modifier.size(22.dp))
            DownloadStatus.Failed -> {
                Icon(Icons.Filled.ErrorOutline, null, tint = colors.destructive, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                IconActionButton(Icons.Filled.Refresh, colors.primary, onRetry)
            }
            DownloadStatus.Downloading -> {
                if (item.fraction == null) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primary)
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
        IconActionButton(Icons.Filled.Close, colors.mutedForeground, onDismiss)
    }
}

@Composable
private fun IconActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Icon(
        icon,
        null,
        tint = tint,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(FlesTheme.radius.full))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(6.dp)
    )
}

private fun itemWord(n: Int) = if (n == 1) "item" else "itens"

private fun progressLabel(item: DownloadItem): String {
    return if (item.totalBytes > 0L) {
        "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}"
    } else if (item.downloadedBytes > 0L) {
        formatBytes(item.downloadedBytes)
    } else {
        "Iniciando…"
    }
}
