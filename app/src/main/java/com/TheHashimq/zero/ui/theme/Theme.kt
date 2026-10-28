/*
 * MODULE: app (theme)
 * PURPOSE: Applies the Zero dark palette and type scale to Material3 components.
 * TAKES: Composable content.
 * GIVES: A MaterialTheme with the Zero colors and typography.
 * OWNS: The color scheme and typography values.
 * DOES NOT OWN: Palette or size values, which come from foundation-design.
 * DEPENDENCIES: foundation-design, Material3.
 * CONTRACT: The app uses only this theme. Font family is the platform default.
 * FAILURE BEHAVIOR: Not applicable.
 */
package com.TheHashimq.zero.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import zero.foundation.design.DesignTokens
import zero.foundation.design.TypeScale

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

private val ZeroTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = TypeScale.DISPLAY_SP.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = TypeScale.HEADLINE_SP.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = TypeScale.TITLE_SP.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = TypeScale.BODY_SP.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = TypeScale.LABEL_SP.sp,
        letterSpacing = 1.sp,
    ),
)

/** Applies the Zero dark theme to [content]. */
@Composable
fun ZeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZeroDarkScheme,
        typography = ZeroTypography,
        content = content,
    )
}
