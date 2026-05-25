package com.archieapps.fles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.share.ShareTargetUiState
import com.archieapps.fles.ui.components.AppInput
import com.archieapps.fles.ui.components.FileIconBadge
import com.archieapps.fles.ui.components.FolderListItem
import com.archieapps.fles.ui.components.IconButtonBox
import com.archieapps.fles.ui.components.flattenChannelTree
import com.archieapps.fles.ui.components.formatBytes
import com.archieapps.fles.ui.model.PendingAttachment
import com.archieapps.fles.ui.theme.FlesTheme

@Composable
fun ShareTargetScreen(
    state: ShareTargetUiState,
    onQueryChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = FlesTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(0.5.dp, colors.border))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonBox(Icons.Filled.Close, onClick = onClose, size = 40.dp, tint = colors.foreground)
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enviar para…", color = colors.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(state.summary, color = colors.mutedForeground, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (state.error != null) {
                Text(
                    state.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = colors.destructive,
                    fontSize = 12.sp
                )
            }
            if (state.attachmentsPreview.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.attachmentsPreview, key = { it.uri.toString() }) { item ->
                        AttachmentPreviewChip(item)
                    }
                }
            }
            AppInput(
                state.query,
                onQueryChange,
                "Buscar pasta",
                leadingIcon = Icons.Filled.Search,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                items(flattenChannelTree(state.channels), key = { it.channel.id }) { item ->
                    FolderListItem(
                        channel = item.channel,
                        active = false,
                        depth = item.depth,
                        onClick = {
                            if (!state.sending) onSend(item.channel.id)
                        }
                    )
                }
            }
        }
        if (state.toast != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(FlesTheme.radius.lg),
                color = if (state.error != null) colors.destructive else colors.primary
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colors.primaryForeground
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(state.toast, color = colors.primaryForeground, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewChip(item: PendingAttachment) {
    val colors = FlesTheme.colors
    Surface(
        shape = RoundedCornerShape(FlesTheme.radius.md),
        color = colors.card,
        border = BorderStroke(0.5.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(FlesTheme.radius.sm))) {
                FileIconBadge(item.kind)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    item.fileName,
                    color = colors.foreground,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.size > 0) {
                    Text(formatBytes(item.size), color = colors.mutedForeground, fontSize = 10.sp)
                }
            }
        }
    }
}
