package com.telebox.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

@Immutable
data class TeleBoxExtendedColors(
    val bg: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val text: Color,
    val subtext: Color,
    val border: Color,
    val hover: Color,
    val glass: Color,
    val authGlass: Color,
    val glassInput: Color,
    val authGradient: Brush,
    val danger: Color,
    val dangerSoft: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val onPrimary: Color,
    val overlayScrim: Color,
    val confirmSurface: Color
)

val LocalTeleBoxColors = staticCompositionLocalOf {
    TeleBoxExtendedColors(
        bg = TelegramBgDark,
        surface = TelegramSurfaceDark,
        primary = TelegramPrimaryDark,
        secondary = TelegramSecondaryDark,
        text = TelegramTextDark,
        subtext = TelegramSubtextDark,
        border = TelegramBorderDark,
        hover = TelegramHoverDark,
        glass = TelegramGlassDark,
        authGlass = TelegramAuthGlassDark,
        glassInput = TelegramGlassInputDark,
        authGradient = SolidColor(AuthGradientEndDark),
        danger = Danger,
        dangerSoft = DangerSoft,
        success = Success,
        warning = Warning,
        info = InfoBlue,
        onPrimary = Color.Black,
        overlayScrim = Color(0x80000000),
        confirmSurface = ConfirmSurface
    )
}

private val DarkExtended = TeleBoxExtendedColors(
    bg = TelegramBgDark,
    surface = TelegramSurfaceDark,
    primary = TelegramPrimaryDark,
    secondary = TelegramSecondaryDark,
    text = TelegramTextDark,
    subtext = TelegramSubtextDark,
    border = TelegramBorderDark,
    hover = TelegramHoverDark,
    glass = TelegramGlassDark,
    authGlass = TelegramAuthGlassDark,
    glassInput = TelegramGlassInputDark,
    authGradient = Brush.radialGradient(
        colors = listOf(AuthGradientStartDark, AuthGradientEndDark)
    ),
    danger = Danger,
    dangerSoft = DangerSoft,
    success = Success,
    warning = Warning,
    info = InfoBlue,
    onPrimary = Color.Black,
    overlayScrim = Color(0x80000000),
    confirmSurface = ConfirmSurface
)

private val LightExtended = TeleBoxExtendedColors(
    bg = TelegramBgLight,
    surface = TelegramSurfaceLight,
    primary = TelegramPrimaryLight,
    secondary = TelegramSecondaryLight,
    text = TelegramTextLight,
    subtext = TelegramSubtextLight,
    border = TelegramBorderLight,
    hover = TelegramHoverLight,
    glass = TelegramGlassLight,
    authGlass = TelegramAuthGlassLight,
    glassInput = TelegramGlassInputLight,
    authGradient = Brush.linearGradient(
        colors = listOf(AuthGradientStartLight, AuthGradientEndLight)
    ),
    danger = Danger,
    dangerSoft = DangerSoft,
    success = Success,
    warning = Warning,
    info = InfoBlue,
    onPrimary = Color.White,
    overlayScrim = Color(0xB3000000),
    confirmSurface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = TelegramPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0x33FFAE00),
    onPrimaryContainer = TelegramPrimaryDark,
    secondary = TelegramSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0x332481CC),
    onSecondaryContainer = TelegramSecondaryDark,
    tertiary = Color(0xFFC084FC),
    background = TelegramBgDark,
    onBackground = TelegramTextDark,
    surface = TelegramSurfaceDark,
    onSurface = TelegramTextDark,
    surfaceVariant = Color(0xFF1E2A36),
    onSurfaceVariant = TelegramSubtextDark,
    outline = TelegramBorderDark,
    outlineVariant = TelegramBorderDark,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Danger,
    inverseSurface = TelegramTextDark,
    inverseOnSurface = TelegramBgDark,
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = TelegramPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0x33E69500),
    onPrimaryContainer = TelegramPrimaryLight,
    secondary = TelegramSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0x332481CC),
    onSecondaryContainer = TelegramSecondaryLight,
    tertiary = Color(0xFF7C3AED),
    background = TelegramBgLight,
    onBackground = TelegramTextLight,
    surface = TelegramSurfaceLight,
    onSurface = TelegramTextLight,
    surfaceVariant = Color(0xFFF7F8FA),
    onSurfaceVariant = TelegramSubtextLight,
    outline = TelegramBorderLight,
    outlineVariant = TelegramBorderLight,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Danger,
    inverseSurface = TelegramTextLight,
    inverseOnSurface = TelegramBgLight,
    scrim = Color.Black
)

@Composable
fun TeleBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extended = if (darkTheme) DarkExtended else LightExtended
    val colorScheme: ColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalTeleBoxColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object TeleBoxTheme {
    val colors: TeleBoxExtendedColors
        @Composable
        get() = LocalTeleBoxColors.current
}

