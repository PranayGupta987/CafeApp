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

private val CafeBrown = Color(0xFF6F4E37)
private val CafeCream = Color(0xFFF5F0E8)
private val CafeGold = Color(0xFFC89B3C)

private val LightColorScheme = lightColorScheme(
    primary = CafeBrown,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7C4A8),
    onPrimaryContainer = Color(0xFF2D1E0F),
    secondary = CafeGold,
    onSecondary = Color.White,
    background = CafeCream,
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0E6D6),
    onSurfaceVariant = Color(0xFF4A4458),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD7C4A8),
    onPrimary = Color(0xFF2D1E0F),
    primaryContainer = CafeBrown,
    onPrimaryContainer = Color(0xFFF5F0E8),
    secondary = CafeGold,
    onSecondary = Color.Black,
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF2B2930),
    onSurface = Color(0xFFE6E1E5),
)

@Composable
fun CafeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        content = content,
    )
}
