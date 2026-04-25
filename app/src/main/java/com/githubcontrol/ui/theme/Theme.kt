package com.githubcontrol.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * All user-tunable visual settings. Defaults match the previous look.
 */
data class ThemeSettings(
    val mode: String = "system",                 // system | light | dark
    val accentKey: String = "blue",              // see AccentPalette
    val dynamicColor: Boolean = false,           // Android 12+ Material You
    val amoled: Boolean = false,                 // pure-black dark backgrounds
    val fontScale: Float = 1.0f,                 // overall typography scale
    val monoFontScale: Float = 1.0f,             // applied to in-app monospace surfaces
    val density: String = "comfortable",         // compact | comfortable | cozy
    val cornerRadius: Int = 14,                  // dp for cards / buttons
    val terminalTheme: String = "github-dark"    // see TerminalPalette
)

/** Density multiplier for vertical spacing throughout the UI. */
val LocalDensityScale = staticCompositionLocalOf { 1.0f }
val LocalMonoScale = staticCompositionLocalOf { 1.0f }
val LocalTerminalTheme = staticCompositionLocalOf { TerminalPalette.byKey("github-dark") }

private fun darkSchemeFor(accent: Color, amoled: Boolean) = darkColorScheme(
    primary = accent, onPrimary = Color.White,
    secondary = GhPurple, onSecondary = if (amoled) GhInkAmoled else GhInk,
    background = if (amoled) GhInkAmoled else GhInk, onBackground = GhText,
    surface = if (amoled) Color(0xFF0A0A0A) else GhSurface, onSurface = GhText,
    surfaceVariant = if (amoled) Color(0xFF111111) else GhSurface2, onSurfaceVariant = GhMuted,
    outline = GhBorder, error = GhDanger
)

private fun lightSchemeFor(accent: Color) = lightColorScheme(
    primary = accent, onPrimary = Color.White,
    secondary = Color(0xFF8250DF), onSecondary = GhSurfaceLight,
    background = GhInkLight, onBackground = GhTextLight,
    surface = GhSurfaceLight, onSurface = GhTextLight,
    surfaceVariant = GhInkLight, onSurfaceVariant = GhMutedLight,
    outline = GhBorderLight, error = GhDanger
)

private fun typographyFor(scale: Float) = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = (34 * scale).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = (26 * scale).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (22 * scale).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (18 * scale).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = (16 * scale).sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = (14 * scale).sp),
    bodyLarge = TextStyle(fontSize = (15 * scale).sp),
    bodyMedium = TextStyle(fontSize = (14 * scale).sp),
    bodySmall = TextStyle(fontSize = (12 * scale).sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (14 * scale).sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = (12 * scale).sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = (11 * scale).sp)
)

private fun shapesFor(corner: Int) = Shapes(
    extraSmall = RoundedCornerShape((corner / 3).dp.coerceAtLeast(2.dp)),
    small = RoundedCornerShape((corner / 2).dp.coerceAtLeast(4.dp)),
    medium = RoundedCornerShape(corner.dp),
    large = RoundedCornerShape((corner + 4).dp),
    extraLarge = RoundedCornerShape((corner + 12).dp)
)

private fun densityScale(density: String): Float = when (density) {
    "compact" -> 0.82f
    "cozy"    -> 1.18f
    else      -> 1.0f
}

@Composable
fun GitHubControlTheme(
    settings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.mode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val context = LocalContext.current
    val accent = AccentPalette.byKey(settings.accentKey)
    val colorScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkSchemeFor(accent, settings.amoled)
        else -> lightSchemeFor(accent)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDensityScale provides densityScale(settings.density),
        LocalMonoScale provides settings.monoFontScale,
        LocalTerminalTheme provides TerminalPalette.byKey(settings.terminalTheme)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typographyFor(settings.fontScale),
            shapes = shapesFor(settings.cornerRadius),
            content = content
        )
    }
}

/** Backwards-compat overload for existing call-sites that only know the mode. */
@Composable
fun GitHubControlTheme(themeMode: String, content: @Composable () -> Unit) =
    GitHubControlTheme(settings = ThemeSettings(mode = themeMode), content = content)
