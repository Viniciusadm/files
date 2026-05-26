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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.security.BiometricAvailability
import com.archieapps.fles.ui.theme.FlesTheme

@Composable
fun AppLockScreen(
    availability: BiometricAvailability,
    onUnlockClick: () -> Unit
) {
    val colors = FlesTheme.colors
    val available = availability == BiometricAvailability.Available

    LaunchedEffect(available) {
        if (available) onUnlockClick()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Aplicativo bloqueado",
                color = colors.foreground,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when (availability) {
                    BiometricAvailability.Available ->
                        "Autentique-se para continuar"
                    BiometricAvailability.NoneEnrolled ->
                        "Configure biometria, PIN ou senha nas Configurações do sistema para desbloquear."
                    BiometricAvailability.NoHardware ->
                        "Este dispositivo não possui autenticação biométrica disponível."
                    BiometricAvailability.Unavailable ->
                        "Autenticação do dispositivo indisponível no momento."
                },
                color = colors.mutedForeground,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            if (available) {
                Button(
                    onClick = onUnlockClick,
                    shape = RoundedCornerShape(FlesTheme.radius.lg),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.primaryForeground
                    )
                ) {
                    Text("Desbloquear", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
