package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AtmosphereDarkScheme = darkColorScheme(
    primary = AtmospherePrimary,
    onPrimary = AtmosphereOnPrimary,
    primaryContainer = AtmospherePrimaryContainer,
    onPrimaryContainer = AtmosphereOnPrimaryContainer,
    secondary = AtmosphereSecondary,
    onSecondary = AtmosphereOnSecondary,
    tertiary = CoolCyan,
    background = AtmosphereBackground,
    onBackground = TextPrimaryLight,
    surface = AtmosphereSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = AtmosphereSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = AtmosphereOutline,
    outlineVariant = Color(0x33FFFFFF),
)

val AtmosphereShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AtmosphereTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AtmosphereDarkScheme,
        typography = AtmosphereTypography,
        shapes = AtmosphereShapes,
        content = content,
    )
}
