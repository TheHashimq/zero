/*
 * MODULE: app (shared components)
 * PURPOSE: Building blocks used across Zero screens.
 * TAKES: Text, colors, enabled flags, and click handlers.
 * GIVES: Section labels, status lines, and primary and secondary buttons.
 * OWNS: No state.
 * DOES NOT OWN: Screen layout or behavior.
 * DEPENDENCIES: Compose Material3, the Zero theme.
 * CONTRACT: Colors and text styles come from the theme. Buttons use the enabled flag exactly as given.
 * FAILURE BEHAVIOR: Not applicable.
 */
package com.TheHashimq.zero.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.util.Locale

/** Uppercase label above a group of content. */
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** One line of status text in a chosen color. */
@Composable
fun StatusLine(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
    )
}

/** Full-width primary action. */
@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text)
    }
}

/** Outlined secondary action. Pass a weight modifier when placing it in a row. */
@Composable
fun SecondaryButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Text(text)
    }
}
