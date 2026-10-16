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
    primary = GeoPurpleLight,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = GeoPurpleContainer,
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    background = GeoDarkBg,
    onBackground = GeoDarkOnBg,
    surface = GeoDarkSurface,
    onSurface = GeoDarkOnSurface,
    surfaceVariant = GeoDarkSurfaceVariant,
    onSurfaceVariant = GeoDarkOnSurfaceVariant,
    outline = GeoDarkOutline,
    outlineVariant = GeoDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = GeoPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = GeoPurpleContainer,
    onPrimaryContainer = GeoPurpleDark,
    secondary = GeoSecondary,
    onSecondary = Color.White,
    secondaryContainer = GeoSecondaryContainer,
    onSecondaryContainer = GeoOnSecondaryContainer,
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    background = GeoLightBg,
    onBackground = GeoLightOnBg,
    surface = GeoLightSurface,
    onSurface = GeoLightOnSurface,
    surfaceVariant = GeoLightSurfaceVariant,
    onSurfaceVariant = GeoLightOnSurfaceVariant,
    outline = GeoLightOutline,
    outlineVariant = GeoLightOutlineVariant
)

@Composable
fun StedifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent Geometric Balance aesthetic
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
