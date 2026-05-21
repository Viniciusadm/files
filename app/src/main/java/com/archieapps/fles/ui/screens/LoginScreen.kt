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
import com.archieapps.fles.ui.viewmodel.LoginUiState

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onKeyClick: () -> Unit
) {
    val colors = FlesTheme.colors
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
                    AppInput(state.email, onEmailChange, "email@email.com")
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Senha", color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    AppInput(state.password, onPasswordChange, "password", visualTransformation = PasswordVisualTransformation())
                }
                if (state.error != null) {
                    Text(state.error, color = colors.destructive, fontSize = 12.sp)
                }
                AppButton(
                    text = "Entrar",
                    onClick = onLoginClick,
                    loading = state.loading,
                    enabled = !state.loading,
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
