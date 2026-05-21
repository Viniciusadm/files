package com.archieapps.fles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalFlesColors = staticCompositionLocalOf { LightFlesColors }

object FlesTheme {
    val colors: FlesColors
        @Composable get() = LocalFlesColors.current

    object radius {
        val sm = 8.dp
        val md = 10.dp
        val lg = 12.dp
        val xl = 16.dp
        val x2l = 20.dp
        val x3l = 24.dp
        val x4l = 28.dp
        val full = 9999.dp
    }
}

@Composable
fun FlesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkFlesColors else LightFlesColors
    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.primaryForeground,
            background = appColors.background,
            onBackground = appColors.foreground,
            surface = appColors.card,
            onSurface = appColors.cardForeground,
            error = appColors.destructive,
            onError = appColors.destructiveForeground
        )
    } else {
        lightColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.primaryForeground,
            background = appColors.background,
            onBackground = appColors.foreground,
            surface = appColors.card,
            onSurface = appColors.cardForeground,
            error = appColors.destructive,
            onError = appColors.destructiveForeground
        )
    }

    CompositionLocalProvider(LocalFlesColors provides appColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}
