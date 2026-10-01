package com.aura.weight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.aura.weight.core.LocalGlass

val LocalAuraColors = staticCompositionLocalOf { DarkColors }
val LocalIsDark = staticCompositionLocalOf { true }

@Composable
fun AuraTheme(
    dark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (dark) DarkColors else LightColors
    val glass = if (dark) DarkGlass else LightGlass
    CompositionLocalProvider(
        LocalAuraColors provides colors,
        LocalIsDark provides dark,
        LocalGlass provides glass,
    ) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(
                    primary = colors.accent,
                    secondary = colors.accent2,
                    tertiary = colors.accent3,
                    background = InkBlack,
                    surface = colors.surface,
                    onPrimary = colors.onAccent,
                    onBackground = colors.primary,
                    onSurface = colors.primary,
                )
            } else {
                lightColorScheme(
                    primary = colors.accent,
                    secondary = colors.accent2,
                    tertiary = colors.accent3,
                    background = colors.surface,
                    surface = colors.surfaceHigh,
                    onPrimary = colors.onAccent,
                    onBackground = colors.primary,
                    onSurface = colors.primary,
                )
            },
            content = content,
        )
    }
}
