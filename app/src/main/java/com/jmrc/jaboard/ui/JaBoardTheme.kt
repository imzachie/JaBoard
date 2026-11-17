package com.jmrc.jaboard.ui

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

private val CobaltLight = lightColorScheme(
    primary = Color(0xFF1A56DB), onPrimary = Color.White, primaryContainer = Color(0xFFDCE5FF), onPrimaryContainer = Color(0xFF001849),
    secondary = Color(0xFF4B5E82), onSecondary = Color.White, secondaryContainer = Color(0xFFD9E2FF), onSecondaryContainer = Color(0xFF081B37),
    tertiary = Color(0xFF0F766E), onTertiary = Color.White, tertiaryContainer = Color(0xFF9CF2E7), onTertiaryContainer = Color(0xFF00201D),
    background = Color(0xFFF6F8FC), onBackground = Color(0xFF171B24), surface = Color(0xFFF6F8FC), onSurface = Color(0xFF171B24),
    surfaceDim = Color(0xFFD8DEE9), surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF0F3FA), surfaceContainer = Color(0xFFEAEFF7),
    surfaceContainerHigh = Color(0xFFE5EAF3), surfaceContainerHighest = Color(0xFFDEE4EF),
    surfaceVariant = Color(0xFFE8ECF4), onSurfaceVariant = Color(0xFF454A54), outline = Color(0xFF747985)
)
private val CobaltDark = darkColorScheme(
    primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF092E6B), primaryContainer = Color(0xFF17458F), onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFBEC6DC), onSecondary = Color(0xFF283349), secondaryContainer = Color(0xFF3A465E), onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = Color(0xFF7DD3C8), onTertiary = Color(0xFF003731), tertiaryContainer = Color(0xFF005048), onTertiaryContainer = Color(0xFF9CF2E7),
    background = Color(0xFF10131A), onBackground = Color(0xFFE2E5EF), surface = Color(0xFF10131A), onSurface = Color(0xFFE2E5EF),
    surfaceDim = Color(0xFF10131A), surfaceBright = Color(0xFF363C47),
    surfaceContainerLowest = Color(0xFF0B0F15), surfaceContainerLow = Color(0xFF171B23), surfaceContainer = Color(0xFF1B2029),
    surfaceContainerHigh = Color(0xFF252B36), surfaceContainerHighest = Color(0xFF303642),
    surfaceVariant = Color(0xFF282D38), onSurfaceVariant = Color(0xFFC3C7D2), outline = Color(0xFF8D919C)
)

@Composable
fun JaBoardTheme(theme: String, dynamicColor: Boolean, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) { "Light" -> false; "Dark" -> true; else -> systemDark }
    val context = LocalContext.current
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) CobaltDark else CobaltLight
    MaterialTheme(colorScheme = colors, content = content)
}
