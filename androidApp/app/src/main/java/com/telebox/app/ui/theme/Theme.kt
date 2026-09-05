package com.telebox.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.core.view.WindowCompat

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
    primaryContainer = Color(0xFF3D2E00),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = TelegramSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF16324A),
    onSecondaryContainer = Color(0xFFD7ECFF),
    tertiary = Color(0xFFC084FC),
    background = TelegramBgDark,
    onBackground = TelegramTextDark,
    surface = TelegramSurfaceDark,
    onSurface = TelegramTextDark,
    surfaceVariant = Color(0xFF1E2A36),
    onSurfaceVariant = TelegramSubtextDark,
    surfaceContainerLowest = Color(0xFF0B121A),
    surfaceContainerLow = Color(0xFF121B24),
    surfaceContainer = Color(0xFF17212B),
    surfaceContainerHigh = Color(0xFF1E2A36),
    surfaceContainerHighest = Color(0xFF243140),
    outline = Color(0xFF4A5B6B),
    outlineVariant = Color(0xFF2C3A48),
    error = Danger,
    onError = Color.White,
    errorContainer = Color(0xFF5C1616),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = TelegramTextDark,
    inverseOnSurface = TelegramBgDark,
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = TelegramPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE08A),
    onPrimaryContainer = Color(0xFF3D2E00),
    secondary = TelegramSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7ECFF),
    onSecondaryContainer = Color(0xFF0B2A44),
    tertiary = Color(0xFF7C3AED),
    background = TelegramBgLight,
    onBackground = TelegramTextLight,
    surface = TelegramSurfaceLight,
    onSurface = TelegramTextLight,
    surfaceVariant = Color(0xFFF1F4F8),
    onSurfaceVariant = TelegramSubtextLight,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainer = Color(0xFFEEF1F5),
    surfaceContainerHigh = Color(0xFFE6EAEF),
    surfaceContainerHighest = Color(0xFFDDE3EA),
    outline = Color(0xFFC5CDD6),
    outlineVariant = Color(0xFFE2E8EE),
    error = Danger,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = TelegramTextLight,
    inverseOnSurface = TelegramBgLight,
    scrim = Color.Black
)

private val TeleBoxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TeleBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extended = if (darkTheme) DarkExtended else LightExtended
    val colorScheme: ColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalTeleBoxColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = TeleBoxShapes,
            content = content
        )
    }
}

object TeleBoxTheme {
    val colors: TeleBoxExtendedColors
        @Composable
        get() = LocalTeleBoxColors.current
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
