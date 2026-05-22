package com.archieapps.fles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.components.AppButton
import com.archieapps.fles.ui.components.AppButtonVariant
import com.archieapps.fles.ui.components.IconButtonBox
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.viewmodel.SecureFoldersUiState

@Composable
fun SecureFoldersScreen(
    state: SecureFoldersUiState,
    openDrawer: () -> Unit,
    onPinChange: (String) -> Unit,
    onSetup: () -> Unit,
    onUnlock: () -> Unit,
    onReset: () -> Unit,
    onLock: () -> Unit
) {
    val colors = FlesTheme.colors
    val pairs = remember(state.configured, state.unlocked) {
        (0..9).shuffled().chunked(2).map { it.sorted() }
    }
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonBox(Icons.Filled.Menu, onClick = openDrawer)
            Text(
                "Pastas Seguras",
                color = colors.foreground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            if (state.loading) {
                CircularProgressIndicator(color = colors.primary)
            } else if (state.unlocked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = colors.primaryForeground)
                    }
                    Text("Pastas seguras desbloqueadas", color = colors.foreground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("A seção segura já está disponível no menu.", color = colors.mutedForeground, fontSize = 14.sp)
                    AppButton("Bloquear agora", variant = AppButtonVariant.Secondary, onClick = onLock)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = colors.primaryForeground)
                    }
                    Text(
                        if (state.configured) "Digite o PIN" else "Configure um PIN",
                        color = colors.foreground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    PinDots(count = state.pin.length)
                    SecureKeypad(
                        pairs = pairs,
                        pin = state.pin,
                        onPinChange = onPinChange
                    )
                    state.error?.let { Text(it, color = colors.destructive, fontSize = 13.sp) }
                    AppButton(
                        if (state.configured) "Desbloquear" else "Salvar PIN",
                        enabled = state.pin.length == 4,
                        onClick = if (state.configured) onUnlock else onSetup
                    )
                    if (state.configured) {
                        AppButton("Resetar", variant = AppButtonVariant.Ghost, enabled = state.pin.length == 4, onClick = onReset)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDots(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (index < count) FlesTheme.colors.primary else FlesTheme.colors.border)
            )
        }
    }
}

@Composable
private fun SecureKeypad(
    pairs: List<List<Int>>,
    pin: String,
    onPinChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pairs.forEach { pair ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(FlesTheme.radius.md),
                color = FlesTheme.colors.card,
                border = BorderStroke(1.dp, FlesTheme.colors.border)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    pair.forEachIndexed { index, digit ->
                        Text(
                            text = if (index == 0) "$digit ou" else "$digit",
                            color = FlesTheme.colors.foreground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = pin.length < 4) { onPinChange(pin + digit.toString()) }
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        )
                    }
                }
            }
        }
        AppButton(
            "Apagar",
            leadingIcon = Icons.Filled.Backspace,
            variant = AppButtonVariant.Secondary,
            enabled = pin.isNotEmpty(),
            onClick = { onPinChange(pin.dropLast(1)) }
        )
    }
}
