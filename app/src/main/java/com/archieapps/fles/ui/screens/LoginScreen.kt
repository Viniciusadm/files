package com.archieapps.fles.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.components.AppButton
import com.archieapps.fles.ui.components.AppCard
import com.archieapps.fles.ui.components.AppInput
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha

@Composable
fun LoginScreen(onEnter: () -> Unit, onKeyClick: () -> Unit) {
    val colors = FlesTheme.colors
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 384.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Entrar", color = colors.foreground, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
                Text("Acesse seu espaço pessoal de arquivos.", color = colors.mutedForeground, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Email", color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    AppInput(email, { email = it }, "voce@exemplo.com")
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Senha", color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    AppInput(password, { password = it }, "", visualTransformation = PasswordVisualTransformation())
                }
                if (error != null) {
                    Text(error ?: "", color = colors.destructive, fontSize = 12.sp)
                }
                AppButton(
                    text = "Entrar",
                    onClick = {
                        if (email.trim().isEmpty() || password.isEmpty()) {
                            error = "Informe email e senha"
                        } else {
                            error = null
                            onEnter()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.background(colors.favorite.withAlpha(0.12f)).padding(12.dp)) {
                    Text("Chave E2EE não configurada", color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Sem ela, o app não consegue criptografar nem ler dados.", color = colors.mutedForeground, fontSize = 12.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    AppButton("Configurar agora", onClick = onKeyClick, leadingIcon = Icons.Filled.Key)
                }
            }
        }
    }
}
