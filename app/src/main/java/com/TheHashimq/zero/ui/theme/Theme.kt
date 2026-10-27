/*
 * MODULE: app (theme)
 * PURPOSE: Applies the Zero dark palette to Material3 components.
 * TAKES: Composable content.
 * GIVES: A MaterialTheme with the Zero color scheme.
 * OWNS: The color scheme value.
 * DOES NOT OWN: Palette values, which come from foundation-design.
 * DEPENDENCIES: foundation-design, Material3.
 * CONTRACT: The app uses only this theme; no other color scheme is applied.
 * FAILURE BEHAVIOR: Not applicable.
 */
package com.TheHashimq.zero.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import zero.foundation.design.DesignTokens

private val ZeroDarkScheme = darkColorScheme(
    background = Color(DesignTokens.BACKGROUND),
    surface = Color(DesignTokens.SURFACE),
    surfaceVariant = Color(DesignTokens.ELEVATED),
    primary = Color(DesignTokens.ACCENT),
    onPrimary = Color(DesignTokens.BACKGROUND),
    secondary = Color(DesignTokens.ACCENT_BRIGHT),
    onBackground = Color(DesignTokens.TEXT_PRIMARY),
    onSurface = Color(DesignTokens.TEXT_PRIMARY),
    onSurfaceVariant = Color(DesignTokens.TEXT_SECONDARY),
    outline = Color(DesignTokens.BORDER),
    error = Color(DesignTokens.CRITICAL),
)

/** Applies the Zero dark theme to [content]. */
@Composable
fun ZeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ZeroDarkScheme, typography = Typography, content = content)
}
