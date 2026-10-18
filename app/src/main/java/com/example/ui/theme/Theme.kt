package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = OchreSecondaryDark,
    onSecondary = OchreOnSecondaryDark,
    secondaryContainer = OchreSecondaryContainerDark,
    onSecondaryContainer = OchreOnSecondaryContainerDark,
    tertiary = RiverTertiaryDark,
    onTertiary = RiverOnTertiaryDark,
    tertiaryContainer = RiverTertiaryContainerDark,
    onTertiaryContainer = RiverOnTertiaryContainerDark,
    error = DebtErrorDark,
    background = SurfaceDark,
    surface = SurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldPrimaryContainerLight,
    onPrimaryContainer = EmeraldOnPrimaryContainerLight,
    secondary = OchreSecondaryLight,
    onSecondary = OchreOnSecondaryLight,
    secondaryContainer = OchreSecondaryContainerLight,
    onSecondaryContainer = OchreOnSecondaryContainerLight,
    tertiary = RiverTertiaryLight,
    onTertiary = RiverOnTertiaryLight,
    tertiaryContainer = RiverTertiaryContainerLight,
    onTertiaryContainer = RiverOnTertiaryContainerLight,
    error = DebtErrorLight,
    background = SurfaceLight,
    surface = SurfaceLight
)

@Composable
fun TripSplitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve distinct high-contrast tour brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
