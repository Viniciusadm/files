package com.archieapps.fles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.security.BiometricAvailability
import com.archieapps.fles.ui.components.CreateChannelDialog
import com.archieapps.fles.ui.components.IconButtonBox
import com.archieapps.fles.ui.theme.FlesTheme

@Composable
fun SettingsScreen(
    secureUnlocked: Boolean,
    appLockEnabled: Boolean,
    appLockAvailability: BiometricAvailability,
    onBack: () -> Unit,
    onCreateChannel: (String, String, String, Boolean) -> Unit,
    onNavigateKey: () -> Unit,
    onNavigateSecureFolders: () -> Unit,
    onEnableAppLock: () -> Unit,
    onDisableAppLock: () -> Unit,
    onRefreshAppLockAvailability: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = FlesTheme.colors
    var createOpen by remember { mutableStateOf(false) }
    var appLockDialog by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonBox(Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
            Spacer(Modifier.width(8.dp))
            Text(
                "Configurações",
                color = colors.foreground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileCard()
            SettingsItem(
                icon = Icons.Filled.Add,
                title = "Nova pasta",
                subtitle = "Criar uma pasta de conversa",
                onClick = { createOpen = true }
            )
            SettingsItem(
                icon = Icons.Filled.Key,
                title = "Chave de criptografia",
                subtitle = "Configurar ou regenerar sua chave",
                onClick = onNavigateKey
            )
            SettingsItem(
                icon = if (secureUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                title = "Pastas seguras",
                subtitle = if (secureUnlocked) "Desbloqueado" else "Bloqueado",
                onClick = onNavigateSecureFolders
            )
            SettingsItem(
                icon = Icons.Filled.Fingerprint,
                title = "Bloqueio do aplicativo",
                subtitle = when {
                    appLockAvailability != BiometricAvailability.Available && !appLockEnabled ->
                        "Indisponível neste dispositivo"
                    appLockEnabled -> "Ativado"
                    else -> "Desativado"
                },
                onClick = {
                    onRefreshAppLockAvailability()
                    appLockDialog = true
                }
            )
            SettingsItem(
                icon = Icons.Filled.Logout,
                title = "Sair",
                subtitle = null,
                destructive = true,
                onClick = onLogout
            )
        }
    }
    if (createOpen) {
        CreateChannelDialog(
            secureEnabled = secureUnlocked,
            onClose = { createOpen = false },
            onCreate = { name, icon, color, isSecure ->
                onCreateChannel(name, icon, color, isSecure)
                createOpen = false
            }
        )
    }
    if (appLockDialog) {
        AppLockDialog(
            enabled = appLockEnabled,
            availability = appLockAvailability,
            onDismiss = { appLockDialog = false },
            onEnable = {
                appLockDialog = false
                onEnableAppLock()
            },
            onDisable = {
                appLockDialog = false
                onDisableAppLock()
            }
        )
    }
}

@Composable
private fun AppLockDialog(
    enabled: Boolean,
    availability: BiometricAvailability,
    onDismiss: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit
) {
    val colors = FlesTheme.colors
    val available = availability == BiometricAvailability.Available
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text("Bloqueio do aplicativo", color = colors.foreground) },
        text = {
            Text(
                text = when {
                    !available && !enabled -> when (availability) {
                        BiometricAvailability.NoneEnrolled ->
                            "Configure biometria, PIN, senha ou padrão de desbloqueio nas Configurações do sistema para usar este recurso."
                        BiometricAvailability.NoHardware ->
                            "Este dispositivo não possui autenticação biométrica disponível."
                        else ->
                            "Autenticação do dispositivo indisponível no momento."
                    }
                    enabled ->
                        "Ao desativar, o aplicativo não exigirá mais autenticação local ao abrir."
                    else ->
                        "Ao ativar, o aplicativo exigirá biometria ou credencial do dispositivo para ser aberto."
                },
                color = colors.mutedForeground
            )
        },
        confirmButton = {
            when {
                enabled -> TextButton(onClick = onDisable) {
                    Text("Desativar", color = colors.destructive)
                }
                available -> TextButton(onClick = onEnable) {
                    Text("Ativar", color = colors.primary)
                }
                else -> TextButton(onClick = onDismiss) {
                    Text("Entendi", color = colors.primary)
                }
            }
        },
        dismissButton = if (enabled || available) {
            { TextButton(onClick = onDismiss) { Text("Cancelar", color = colors.mutedForeground) } }
        } else null
    )
}

@Composable
private fun ProfileCard() {
    val colors = FlesTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FlesTheme.radius.xl),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("U", color = colors.primaryForeground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("usuario", color = colors.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("usuario@exemplo.com", color = colors.mutedForeground, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val colors = FlesTheme.colors
    val fg = if (destructive) colors.destructive else colors.foreground
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(FlesTheme.radius.xl),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(FlesTheme.radius.md))
                    .background(if (destructive) colors.destructive.copy(alpha = 0.1f) else colors.muted),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, color = colors.mutedForeground, fontSize = 12.sp)
                }
            }
            Icon(Icons.Filled.ChevronRight, null, tint = colors.mutedForeground, modifier = Modifier.size(20.dp))
        }
    }
}
