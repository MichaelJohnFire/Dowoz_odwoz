package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BusDarkPrimary,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = BusDarkBackground,
    surface = BusDarkSurface,
    surfaceVariant = BusDarkSurfaceVariant,
    outline = Color(0xFF475569),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8),
)

private val LightColorScheme = lightColorScheme(
    primary = BusPrimary,
    onPrimary = BusOnPrimary,
    primaryContainer = BusPrimaryContainer,
    onPrimaryContainer = BusOnPrimaryContainer,
    secondary = BusSecondary,
    onSecondary = BusOnSecondary,
    secondaryContainer = BusSecondaryContainer,
    onSecondaryContainer = BusOnSecondaryContainer,
    tertiary = BusTertiary,
    onTertiary = BusOnTertiary,
    tertiaryContainer = BusTertiaryContainer,
    onTertiaryContainer = BusOnTertiaryContainer,
    background = BusBackground,
    surface = BusSurface,
    surfaceVariant = BusSurfaceVariant,
    outline = BusOutline,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
)

@Composable
fun DowozOdwozTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep intentional school theme for consistency
    content: @Composable () -> Unit,
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
