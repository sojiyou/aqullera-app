package com.example.aquallera.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = MidnightBlue,
    onPrimary = AppBg,
    primaryContainer = MidnightBlueActive,
    secondary = Blue,
    onSecondary = AppBg,
    tertiary = LightYellow,
    background = AppBg,
    surface = AppBg,
    surfaceVariant = MistGray,
    error = StatusCancelled,
)

@Composable
fun AqualleraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content,
    )
}
