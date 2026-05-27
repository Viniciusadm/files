package com.archieapps.fles.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha

enum class MainSection {
    Messages,
    Gallery,
    Files,
    Settings,
    Key,
    SecureFolders,
    Search,
    Share
}

@Composable
fun AppShell(
    section: MainSection,
    onNavigateMessages: () -> Unit,
    onNavigateGallery: () -> Unit,
    onNavigateFiles: () -> Unit,
    showBottomNav: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(FlesTheme.colors.background)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        ) {
            content()
        }
        if (showBottomNav) {
            val currentTab = when (section) {
                MainSection.Messages -> MainTab.Messages
                MainSection.Gallery -> MainTab.Gallery
                MainSection.Files -> MainTab.Files
                else -> null
            }
            if (currentTab != null) {
                FloatingBottomNav(
                    current = currentTab,
                    onSelect = { selected ->
                        when (selected) {
                            MainTab.Messages -> onNavigateMessages()
                            MainTab.Gallery -> onNavigateGallery()
                            MainTab.Files -> onNavigateFiles()
                        }
                    }
                )
            }
        }
        // Faixa de downloads: ancorada na base, acima da navegação flutuante quando ela existe.
        DownloadsBanner(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(bottom = if (showBottomNav) 92.dp else 12.dp)
        )
    }
}

@Composable
fun CreateChannelDialog(
    secureEnabled: Boolean,
    onClose: () -> Unit,
    onCreate: (String, String, String, Boolean) -> Unit,
    title: String = "Nova pasta",
    confirmLabel: String = "Criar pasta",
    secureOptionVisible: Boolean = secureEnabled,
    initialSecure: Boolean = false
) {
    val colors = FlesTheme.colors
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("Folder") }
    var color by remember { mutableStateOf("#3390ec") }
    var isSecure by remember { mutableStateOf(initialSecure) }
    val icons = listOf("Folder", "Inbox", "Briefcase", "GraduationCap", "Image", "Star")
    val colorsList = listOf("#3390ec", "#22c55e", "#eab308", "#ef4444", "#8b5cf6", "#06b6d4")
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppInput(name, { name = it }, "Ex.: Receitas")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    icons.forEach { item ->
                        IconButtonBox(
                            icon = folderIcon(item),
                            onClick = { icon = item },
                            background = if (icon == item) colors.primary.withAlpha(0.12f) else Color.Transparent,
                            tint = if (icon == item) colors.primary else colors.mutedForeground
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorsList.forEach { item ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(parseHexColor(item))
                                .border(BorderStroke(if (color == item) 3.dp else 0.dp, colors.foreground), androidx.compose.foundation.shape.CircleShape)
                                .clickable { color = item }
                        )
                    }
                }
                if (secureOptionVisible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isSecure, onCheckedChange = { isSecure = it })
                        Text("Segura", color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = { AppButton(confirmLabel, enabled = name.trim().isNotBlank(), onClick = { onCreate(name, icon, color, isSecure) }) },
        dismissButton = { AppButton("Cancelar", variant = AppButtonVariant.Ghost, onClick = onClose) }
    )
}

private fun parseHexColor(value: String): Color {
    return runCatching { Color(("FF" + value.removePrefix("#")).toLong(16)) }.getOrDefault(Color(0xFF3390EC))
}
