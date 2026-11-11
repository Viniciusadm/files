package com.archieapps.fles.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.components.AppButton
import com.archieapps.fles.ui.components.AppButtonVariant
import com.archieapps.fles.ui.components.AppCard
import com.archieapps.fles.ui.components.IconButtonBox
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha
import com.archieapps.fles.ui.viewmodel.ConnectComputerUiState

@Composable
fun ConnectComputerScreen(
    state: ConnectComputerUiState,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onConnect: () -> Unit,
    onConnectAnother: () -> Unit,
    onConfigureKey: () -> Unit
) {
    val colors = FlesTheme.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButtonBox(Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = onBack, iconSize = 22.dp)
            Spacer(Modifier.width(4.dp))
            Text("Conectar computador", color = colors.foreground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.primary.withAlpha(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (state.connected) Icons.Filled.CheckCircle else Icons.Filled.Computer, null, tint = colors.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (state.connected) "Computador conectado" else "Envie sua chave para o computador",
                            color = colors.cardForeground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    when {
                        !state.hasKey -> {
                            Text("Configure a chave de criptografia neste celular antes de conectar um computador.", color = colors.mutedForeground, fontSize = 14.sp, lineHeight = 20.sp)
                            AppButton("Configurar chave", onClick = onConfigureKey, modifier = Modifier.fillMaxWidth())
                        }
                        state.connected -> {
                            Text(
                                "${state.connectedDevice ?: "O computador"} já pode ver e enviar arquivos. Confira se a impressão digital mostrada lá é a mesma:",
                                color = colors.mutedForeground,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            Text(state.fingerprint.orEmpty(), color = colors.foreground, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                            AppButton("Concluir", onClick = onBack, modifier = Modifier.fillMaxWidth())
                            AppButton("Conectar outro computador", onClick = onConnectAnother, modifier = Modifier.fillMaxWidth(), variant = AppButtonVariant.Outline)
                        }
                        else -> {
                            Text(
                                "No computador, abra o Fles no navegador (ou rode \"fles pair\" no terminal) e digite aqui o código que aparecer. A chave vai criptografada direto para ele.",
                                color = colors.mutedForeground,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            OutlinedTextField(
                                value = state.code,
                                onValueChange = onCodeChange,
                                singleLine = true,
                                placeholder = { Text("XXXXX-XXXXX", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace) },
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 22.sp, letterSpacing = 3.sp, textAlign = TextAlign.Center, color = colors.foreground),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onConnect() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.border,
                                    cursorColor = colors.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (state.error != null) {
                                Text(state.error, color = colors.destructive, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                            AppButton(
                                "Conectar",
                                onClick = onConnect,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.code.count { it.isLetterOrDigit() } == 10,
                                loading = state.loading
                            )
                        }
                    }
                }
            }
        }
    }
}
