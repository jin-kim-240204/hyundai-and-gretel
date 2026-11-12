package com.jinkim.hyundaiAndGretel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalPleosColors = staticCompositionLocalOf { PleosLightColors }

object PleosTheme {
    val colors: PleosColors
        @Composable get() = LocalPleosColors.current
}

@Composable
fun HyundaiAndGretelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) PleosDarkColors else PleosLightColors
    val scheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalPleosColors provides colors) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = colors.buttonFilled, onPrimary = colors.buttonFilledText,
                background = colors.background, onBackground = colors.textPrimary,
                surface = colors.surface, onSurface = colors.textPrimary,
                onSurfaceVariant = colors.textSecondary, outlineVariant = colors.divider,
            ),
            typography = Typography,
            content = content,
        )
    }
}
