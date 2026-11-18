package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RaniDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = TextPrimary,
    primaryContainer = DeepRoyalPurple,
    onPrimaryContainer = SoftLavender,
    secondary = CyberCyan,
    onSecondary = ObsidianVoid,
    secondaryContainer = ColorTokens.CyanContainer,
    onSecondaryContainer = CyberCyan,
    tertiary = NeonLilac,
    onTertiary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianCardElevated,
    outline = SubtleBorder,
    outlineVariant = GlassBorder,
    error = ErrorRose,
    onError = TextPrimary
)

private object ColorTokens {
    val CyanContainer = androidx.compose.ui.graphics.Color(0xFF0A2E33)
}

val RaniShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun RaniAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RaniDarkColorScheme,
        typography = RaniTypography,
        shapes = RaniShapes,
        content = content
    )
}
