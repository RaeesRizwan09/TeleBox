package com.telebox.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
    val confirmSurface: Color,
    val brand: Color,
    val fileImage: Color,
    val fileVideo: Color,
    val fileAudio: Color,
    val filePdf: Color,
    val fileDocument: Color,
    val fileText: Color,
    val fileSpreadsheet: Color,
    val filePresentation: Color,
    val fileArchive: Color,
    val fileCode: Color,
    val fileFolder: Color,
    val fileGeneric: Color
)

val LocalTeleBoxColors = staticCompositionLocalOf {
    TeleBoxExtendedColors(
        bg = BackgroundDark,
        surface = SurfaceContainerDark,
        primary = PrimaryDark,
        secondary = SecondaryDark,
        text = OnSurfaceDark,
        subtext = OnSurfaceVariantDark,
        border = OutlineVariantDark,
        hover = SurfaceContainerHighDark,
        glass = SurfaceContainerDark.copy(alpha = 0.92f),
        authGlass = SurfaceContainerDark.copy(alpha = 0.88f),
        glassInput = SurfaceContainerLowestDark.copy(alpha = 0.72f),
        authGradient = SolidColor(AuthGradientEndDark),
        danger = ErrorDark,
        dangerSoft = ErrorDark.copy(alpha = 0.16f),
        success = SuccessDark,
        warning = WarningDark,
        info = InfoBlue,
        onPrimary = OnPrimaryDark,
        overlayScrim = Color(0x99000000),
        confirmSurface = ConfirmSurface,
        brand = Brand,
        fileImage = FileImageDark,
        fileVideo = FileVideoDark,
        fileAudio = FileAudioDark,
        filePdf = FilePdfDark,
        fileDocument = FileDocumentDark,
        fileText = FileTextDark,
        fileSpreadsheet = FileSpreadsheetDark,
        filePresentation = FilePresentationDark,
        fileArchive = FileArchiveDark,
        fileCode = FileCodeDark,
        fileFolder = FileFolderDark,
        fileGeneric = FileGenericDark
    )
}

private val DarkExtended = TeleBoxExtendedColors(
    bg = BackgroundDark,
    surface = SurfaceContainerDark,
    primary = PrimaryDark,
    secondary = SecondaryDark,
    text = OnSurfaceDark,
    subtext = OnSurfaceVariantDark,
    border = OutlineVariantDark,
    hover = SurfaceContainerHighDark,
    glass = SurfaceContainerDark.copy(alpha = 0.92f),
    authGlass = SurfaceContainerDark.copy(alpha = 0.88f),
    glassInput = SurfaceContainerLowestDark.copy(alpha = 0.72f),
    authGradient = Brush.linearGradient(
        colors = listOf(AuthGradientStartDark, AuthGradientEndDark)
    ),
    danger = ErrorDark,
    dangerSoft = ErrorDark.copy(alpha = 0.16f),
    success = SuccessDark,
    warning = WarningDark,
    info = InfoBlue,
    onPrimary = OnPrimaryDark,
    overlayScrim = Color(0x99000000),
    confirmSurface = ConfirmSurface,
    brand = Brand,
    fileImage = FileImageDark,
    fileVideo = FileVideoDark,
    fileAudio = FileAudioDark,
    filePdf = FilePdfDark,
    fileDocument = FileDocumentDark,
    fileText = FileTextDark,
    fileSpreadsheet = FileSpreadsheetDark,
    filePresentation = FilePresentationDark,
    fileArchive = FileArchiveDark,
    fileCode = FileCodeDark,
    fileFolder = FileFolderDark,
    fileGeneric = FileGenericDark
)

private val LightExtended = TeleBoxExtendedColors(
    bg = BackgroundLight,
    surface = SurfaceContainerLowestLight,
    primary = PrimaryLight,
    secondary = SecondaryLight,
    text = OnSurfaceLight,
    subtext = OnSurfaceVariantLight,
    border = OutlineVariantLight,
    hover = SurfaceContainerHighLight,
    glass = SurfaceContainerLowestLight.copy(alpha = 0.92f),
    authGlass = Color.White.copy(alpha = 0.90f),
    glassInput = Color.White.copy(alpha = 0.86f),
    authGradient = Brush.linearGradient(
        colors = listOf(AuthGradientStartLight, AuthGradientEndLight)
    ),
    danger = ErrorLight,
    dangerSoft = ErrorLight.copy(alpha = 0.12f),
    success = Success,
    warning = Warning,
    info = InfoBlue,
    onPrimary = OnPrimaryLight,
    overlayScrim = Color(0xB3000000),
    confirmSurface = Color.White,
    brand = Brand,
    fileImage = FileImage,
    fileVideo = FileVideo,
    fileAudio = FileAudio,
    filePdf = FilePdf,
    fileDocument = FileDocument,
    fileText = FileText,
    fileSpreadsheet = FileSpreadsheet,
    filePresentation = FilePresentation,
    fileArchive = FileArchive,
    fileCode = FileCode,
    fileFolder = FileFolder,
    fileGeneric = FileGeneric
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
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
