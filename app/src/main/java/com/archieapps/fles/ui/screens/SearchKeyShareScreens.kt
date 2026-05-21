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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
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
import com.archieapps.fles.ui.model.MockData
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha

@Composable
fun SearchScreen(openDrawer: () -> Unit, initialQuery: String, onThreadClick: (String) -> Unit) {
    val colors = FlesTheme.colors
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    val results = if (query.trim().length < 2) {
        emptyList()
    } else {
        MockData.messages.mapNotNull { message ->
            val channel = MockData.channels.first { it.id == message.folderId }
            if (message.content.contains(query.trim(), ignoreCase = true) || channel.name.contains(query.trim(), ignoreCase = true)) {
                channel to message
            } else {
                null
            }
        }
    }
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
            AppInput(query, { query = it }, "Buscar mensagens...", leadingIcon = Icons.Filled.Search, modifier = Modifier.weight(1f))
        }
        when {
            query.trim().length < 2 -> EmptyState("Digite ao menos 2 caracteres", "As mensagens que contêm o termo aparecerão aqui.", modifier = Modifier.fillMaxSize(), icon = Icons.Filled.Search)
            results.isEmpty() -> EmptyState("Nada encontrado", "Nenhum resultado para \"${query.trim()}\".", modifier = Modifier.fillMaxSize(), icon = Icons.Filled.Search)
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.second.id }) { (channel, message) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThreadClick(channel.id) }
                            .border(BorderStroke(0.5.dp, colors.border))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        FolderAvatar(channel.name, channel.color, size = 40.dp, icon = folderIcon(channel.icon))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row {
                                Text(channel.name, modifier = Modifier.weight(1f), color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(message.createdAt.substring(8, 10) + "/" + message.createdAt.substring(5, 7), color = colors.mutedForeground, fontSize = 12.sp)
                            }
                            Text(message.content, color = colors.mutedForeground, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyScreen(openDrawer: () -> Unit) {
    val colors = FlesTheme.colors
    var value by remember { mutableStateOf("") }
    var configured by remember { mutableStateOf(true) }
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
                        color = if (configured) Color(0xFF16A34A).withAlpha(0.08f) else colors.destructive.withAlpha(0.08f),
                        border = BorderStroke(1.dp, if (configured) Color(0xFF16A34A).withAlpha(0.4f) else colors.destructive.withAlpha(0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (configured) Icons.Filled.Check else Icons.Filled.Close, null, modifier = Modifier.size(16.dp), tint = if (configured) Color(0xFF16A34A) else colors.destructive)
                                Spacer(Modifier.width(8.dp))
                                Text(if (configured) "Chave configurada" else "Nenhuma chave configurada", color = if (configured) Color(0xFF16A34A) else colors.destructive, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(if (configured) "SHA-256: 79:1A:4C:8E:2F:9B:55:01" else "Sem chave, mensagens e arquivos não podem ser enviados nem lidos.", color = colors.mutedForeground, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chave (base64)", color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        AppInput(value, { value = it }, "MGFiYz...  (32 bytes em base64)", singleLine = false, minHeight = 96.dp)
                    }
                    AppButton("Salvar", onClick = { configured = true }, modifier = Modifier.fillMaxWidth())
                    AppButton("Gerar nova chave", onClick = { value = "bW9jay1hZXMtMjU2LWtleS1iYXNlNjQtMzItYnl0ZXM=" }, modifier = Modifier.fillMaxWidth(), variant = AppButtonVariant.Outline, leadingIcon = Icons.Filled.Refresh)
                    AppButton("Limpar chave", onClick = { configured = false }, modifier = Modifier.fillMaxWidth(), variant = AppButtonVariant.Destructive)
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
fun ShareScreen(onClose: () -> Unit, onThreadClick: (String) -> Unit) {
    val colors = FlesTheme.colors
    var query by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }
    val channels = MockData.channels.filter { it.name.contains(query, ignoreCase = true) }
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
                    Text("https://example.com/product-roadmap", color = colors.mutedForeground, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButtonBox(Icons.Filled.Add, onClick = {}, size = 40.dp, tint = colors.primary)
            }
            LinkPreviewCard()
            AppInput(query, { query = it }, "Buscar pasta", leadingIcon = Icons.Filled.Search, modifier = Modifier.fillMaxWidth().padding(12.dp))
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                items(channels, key = { it.id }) { channel ->
                    FolderListItem(channel, active = false, onClick = {
                        toast = "Enviado!"
                        onThreadClick(channel.id)
                    })
                }
            }
        }
        if (toast != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(FlesTheme.radius.lg),
                color = colors.primary
            ) {
                Text(toast ?: "", modifier = Modifier.padding(12.dp), color = colors.primaryForeground, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LinkPreviewCard() {
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
                Text("example.com", color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text("Product roadmap", color = colors.foreground, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text("Preview mockado do link compartilhado pelo Android.", color = colors.mutedForeground, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2)
            }
        }
    }
}
