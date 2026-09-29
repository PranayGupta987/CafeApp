package com.cafeapp.core.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = CafeBrown,
    onPrimary = Color.White,
    primaryContainer = CafeCreamDark,
    onPrimaryContainer = CafeBrownDark,
    secondary = CafeGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7ECCF),
    onSecondaryContainer = CafeGoldDark,
    background = CafeCreamLight,
    onBackground = NeutralDark,
    surface = Color.White,
    onSurface = NeutralDark,
    surfaceVariant = CafeCream,
    onSurfaceVariant = NeutralMedium,
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorContainer,
    onErrorContainer = StatusError,
    outline = NeutralVariant,
    outlineVariant = NeutralBorder,
)

private val DarkColorScheme = darkColorScheme(
    primary = CafeGoldLight,
    onPrimary = CafeBrownDark,
    primaryContainer = CafeBrown,
    onPrimaryContainer = CafeCream,
    secondary = CafeGold,
    onSecondary = Color.Black,
    secondaryContainer = CafeGoldDark,
    onSecondaryContainer = CafeCream,
    background = Color(0xFF141210),
    onBackground = Color(0xFFECE0D6),
    surface = Color(0xFF1E1B18),
    onSurface = Color(0xFFECE0D6),
    surfaceVariant = Color(0xFF2C2723),
    onSurfaceVariant = Color(0xFFD0C3B7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF988E85),
    outlineVariant = Color(0xFF4C453E),
)

@Composable
fun CafeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Default to custom cafe branding colors
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
        typography = CafeTypography,
        shapes = CafeShapes,
        content = content,
    )
}
