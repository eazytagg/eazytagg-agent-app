package com.eazytagg.agent.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Orange = Color(0xFF009FE3)
val OnOrange = Color.White

private val DarkScheme = darkColorScheme(
    primary = Orange,
    onPrimary = OnOrange,
    primaryContainer = Color(0xFF171754),
    onPrimaryContainer = Color(0xFFE4F5FF),
    background = Color(0xFF121315),
    onBackground = Color.White,
    surface = Color(0xFF1B1D21),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2B2E34),
    onSurfaceVariant = Color(0xFFA3A7B0),
    error = Color(0xFFFF6B7D),
    onError = Color(0xFF2A0008),
    outline = Color(0xFF4A4E57),
)

private val LightScheme = lightColorScheme(
    primary = Orange,
    onPrimary = OnOrange,
    primaryContainer = Color(0xFFE4F5FF),
    onPrimaryContainer = Color(0xFF171754),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF1B1B1D),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1D),
    surfaceVariant = Color(0xFFECF1F9),
    onSurfaceVariant = Color(0xFF6B6F78),
    error = Color(0xFF94112A),
    onError = Color.White,
    outline = Color(0xFFD5D0C7),
)

/** Meaning colours used for scan results and status chips. */
class StatusColors(val okBg: Color, val okFg: Color, val warnBg: Color, val warnFg: Color, val errBg: Color, val errFg: Color)

val LocalStatus = staticCompositionLocalOf {
    StatusColors(Color(0xFFD5F0E2), Color(0xFF14573A), Color(0xFFFBE3C4), Color(0xFFB35A00), Color(0xFFF9D9DE), Color(0xFF94112A))
}

val LocalIsDark = staticCompositionLocalOf { true }

@Composable
fun isDarkFor(theme: String): Boolean = when (theme) {
    "light" -> false
    "system" -> isSystemInDarkTheme()
    else -> true
}

@Composable
fun PDynamicsTheme(dark: Boolean, content: @Composable () -> Unit) {
    val status = if (dark)
        StatusColors(Color(0xFF123D2A), Color(0xFF7FE0B0), Color(0xFF3A2406), Color(0xFFFFC27A), Color(0xFF45101C), Color(0xFFFF9AA9))
    else
        StatusColors(Color(0xFFD5F0E2), Color(0xFF14573A), Color(0xFFFBE3C4), Color(0xFFB35A00), Color(0xFFF9D9DE), Color(0xFF94112A))
    CompositionLocalProvider(LocalStatus provides status, LocalIsDark provides dark) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
    }
}
