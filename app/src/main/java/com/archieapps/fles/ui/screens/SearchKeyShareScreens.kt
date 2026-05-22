package com.archieapps.fles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.components.AppButton
import com.archieapps.fles.ui.components.AppButtonVariant
import com.archieapps.fles.ui.components.AppCard
import com.archieapps.fles.ui.components.AppInput
import com.archieapps.fles.ui.components.Badge
import com.archieapps.fles.ui.components.EmptyState
import com.archieapps.fles.ui.components.FolderAvatar
import com.archieapps.fles.ui.components.FolderListItem
import com.archieapps.fles.ui.components.IconButtonBox
import com.archieapps.fles.ui.components.folderIcon
import com.archieapps.fles.ui.components.flattenChannelTree
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha
import com.archieapps.fles.ui.viewmodel.KeyUiState
import com.archieapps.fles.ui.viewmodel.SearchUiState
import com.archieapps.fles.ui.viewmodel.ShareUiState

@Composable
fun SearchScreen(
    state: SearchUiState,
    openDrawer: () -> Unit,
    onQueryChange: (String) -> Unit,
    onThreadClick: (String) -> Unit
) {
    val colors = FlesTheme.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(0.5.dp, colors.border))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonBox(Icons.Filled.Menu, onClick = openDrawer)
            Spacer(Modifier.width(8.dp))
            AppInput(state.query, onQueryChange, "Buscar mensagens...", leadingIcon = Icons.Filled.Search, modifier = Modifier.weight(1f))
        }
        when {
            state.loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            state.error != null -> EmptyState("Erro ao buscar", state.error, modifier = Modifier.fillMaxSize(), icon = Icons.Filled.ErrorOutline)
            state.query.trim().length < 2 -> EmptyState("Digite ao menos 2 caracteres", "As mensagens que contêm o termo aparecerão aqui.", modifier = Modifier.fillMaxSize(), icon = Icons.Filled.Search)
            state.results.isEmpty() -> EmptyState("Nada encontrado", "Nenhum resultado para \"${state.query.trim()}\".", modifier = Modifier.fillMaxSize(), icon = Icons.Filled.Search)
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.results, key = { it.message.id }) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThreadClick(result.channel.id) }
                            .border(BorderStroke(0.5.dp, colors.border))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        FolderAvatar(result.channel.name, result.channel.color, size = 40.dp, icon = folderIcon(result.channel.icon))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row {
                                Text(result.channel.name, modifier = Modifier.weight(1f), color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(result.message.createdAt.substring(8, 10) + "/" + result.message.createdAt.substring(5, 7), color = colors.mutedForeground, fontSize = 12.sp)
                            }
                            Text(result.message.content, color = colors.mutedForeground, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyScreen(
    state: KeyUiState,
    openDrawer: () -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit
) {
    val colors = FlesTheme.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButtonBox(Icons.Filled.Menu, onClick = openDrawer)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppCard(modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.primary.withAlpha(0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Key, null, tint = colors.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Chave E2EE", color = colors.cardForeground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("Cole uma chave AES-256 em base64 (32 bytes). Ela fica apenas neste dispositivo.", color = colors.mutedForeground, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(FlesTheme.radius.md),
                        color = if (state.configured) Color(0xFF16A34A).withAlpha(0.08f) else colors.destructive.withAlpha(0.08f),
                        border = BorderStroke(1.dp, if (state.configured) Color(0xFF16A34A).withAlpha(0.4f) else colors.destructive.withAlpha(0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (state.configured) Icons.Filled.Check else Icons.Filled.Close, null, modifier = Modifier.size(16.dp), tint = if (state.configured) Color(0xFF16A34A) else colors.destructive)
                                Spacer(Modifier.width(8.dp))
                                Text(if (state.configured) "Chave configurada" else "Nenhuma chave configurada", color = if (state.configured) Color(0xFF16A34A) else colors.destructive, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(if (state.configured) "SHA-256: ${state.fingerprint.orEmpty()}" else "Sem chave, mensagens e arquivos não podem ser enviados nem lidos.", color = colors.mutedForeground, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    if (state.error != null) {
                        Text(state.error, color = colors.destructive, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chave (base64)", color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        AppInput(state.value, onValueChange, "MGFiYz...  (32 bytes em base64)", singleLine = false, minHeight = 96.dp)
                    }
                    AppButton("Salvar", onClick = onSave, modifier = Modifier.fillMaxWidth())
                    AppButton("Gerar nova chave", onClick = onGenerate, modifier = Modifier.fillMaxWidth(), variant = AppButtonVariant.Outline, leadingIcon = Icons.Filled.Refresh)
                    AppButton("Limpar chave", onClick = onClear, modifier = Modifier.fillMaxWidth(), variant = AppButtonVariant.Destructive)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge("AES-256-GCM")
                        Badge("32 bytes")
                        Badge("Base64")
                    }
                    Text("A chave nunca sai deste dispositivo. Para usar o mesmo conteúdo em outro aparelho, copie a chave e cole na mesma tela do outro lado.", color = colors.mutedForeground, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ShareScreen(
    state: ShareUiState,
    onQueryChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onClose: () -> Unit,
    onThreadClick: (String) -> Unit,
    onCreateChannel: (String, String, String) -> Unit
) {
    val colors = FlesTheme.colors
    var createOpen by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().background(colors.background).safeDrawingPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(0.5.dp, colors.border))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonBox(Icons.Filled.Close, onClick = onClose, size = 40.dp, tint = colors.foreground)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enviar para…", color = colors.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(state.summary, color = colors.mutedForeground, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButtonBox(Icons.Filled.Add, onClick = { createOpen = true }, size = 40.dp, tint = colors.primary)
            }
            if (state.error != null) {
                Text(state.error, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = colors.destructive, fontSize = 12.sp)
            }
            LinkPreviewCard(state.previewSite, state.previewTitle, state.previewDescription)
            AppInput(state.query, onQueryChange, "Buscar pasta", leadingIcon = Icons.Filled.Search, modifier = Modifier.fillMaxWidth().padding(12.dp))
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                items(flattenChannelTree(state.channels), key = { it.channel.id }) { item ->
                    FolderListItem(item.channel, active = false, depth = item.depth, onClick = {
                        onSend(item.channel.id)
                    })
                }
            }
        }
        if (createOpen) {
            ShareCreateChannelDialog(
                onClose = { createOpen = false },
                onCreate = { name, icon, color ->
                    onCreateChannel(name, icon, color)
                    createOpen = false
                }
            )
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
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (state.sending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.primaryForeground)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(state.toast, color = colors.primaryForeground, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ShareCreateChannelDialog(onClose: () -> Unit, onCreate: (String, String, String) -> Unit) {
    val colors = FlesTheme.colors
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Nova pasta") },
        text = { AppInput(name, { name = it }, "Ex.: Receitas") },
        confirmButton = { AppButton("Criar pasta", enabled = name.trim().isNotBlank(), onClick = { onCreate(name, "Folder", "#3390ec") }) },
        dismissButton = { AppButton("Cancelar", variant = AppButtonVariant.Ghost, onClick = onClose) },
        containerColor = colors.background
    )
}

@Composable
private fun LinkPreviewCard(site: String?, title: String?, description: String?) {
    if (site == null && title == null && description == null) return
    val colors = FlesTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(FlesTheme.radius.lg),
        color = colors.card,
        border = BorderStroke(0.5.dp, colors.border)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(FlesTheme.radius.md)).background(colors.muted), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Link, null, modifier = Modifier.size(28.dp), tint = colors.mutedForeground)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(site.orEmpty(), color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(title.orEmpty(), color = colors.foreground, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(description.orEmpty(), color = colors.mutedForeground, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2)
            }
        }
    }
}
