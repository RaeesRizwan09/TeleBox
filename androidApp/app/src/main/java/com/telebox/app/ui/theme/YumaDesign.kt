package com.telebox.app.ui.theme

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * Yuma Design System (YDS 2.1) primitives adapted for TeleBox.
 *
 * These mirror the building blocks analysed in YumaPlayer (see SKILL.md):
 *  - segmented glass surfaces with position-aware hairline lighting
 *  - tactile press-scale clickables (0.96f spring) with light haptics
 *  - composite corner radii (22dp outer / 5dp inner) and a 1.5dp segment gap
 *
 * The system is intentionally additive: it reads from TeleBox's existing
 * [TeleBoxTheme] + Material 3 surfaces and never mutates them.
 */

object YumaTokens {
    val SegmentGap = 1.5.dp
    val SegmentOuter = 22.dp
    val SegmentInner = 5.dp
    val SheetRadius = 28.dp
    val CardRadius = 18.dp
    val PressScale = 0.96f
    val TouchTarget = 48.dp
    val GlassBorderThickness = 0.5.dp
}

enum class YumaSegmentPosition { Single, First, Middle, Last }

fun yumaSegmentPosition(index: Int, count: Int): YumaSegmentPosition = when {
    count <= 1 -> YumaSegmentPosition.Single
    index == 0 -> YumaSegmentPosition.First
    index == count - 1 -> YumaSegmentPosition.Last
    else -> YumaSegmentPosition.Middle
}

fun yumaSegmentAlphas(position: YumaSegmentPosition): Pair<Float, Float> = when (position) {
    YumaSegmentPosition.Single -> 0.20f to 0.04f
    YumaSegmentPosition.First -> 0.20f to 0.08f
    YumaSegmentPosition.Middle -> 0.08f to 0.08f
    YumaSegmentPosition.Last -> 0.08f to 0.04f
}

/** Composite corner shape that fuses adjacent rows into one visual group. */
fun segmentedItemShape(index: Int, count: Int): Shape {
    return when (yumaSegmentPosition(index, count)) {
        YumaSegmentPosition.Single -> RoundedCornerShape(YumaTokens.SegmentOuter)
        YumaSegmentPosition.First -> RoundedCornerShape(
            topStart = YumaTokens.SegmentOuter, topEnd = YumaTokens.SegmentOuter,
            bottomStart = YumaTokens.SegmentInner, bottomEnd = YumaTokens.SegmentInner
        )
        YumaSegmentPosition.Middle -> RoundedCornerShape(YumaTokens.SegmentInner)
        YumaSegmentPosition.Last -> RoundedCornerShape(
            topStart = YumaTokens.SegmentInner, topEnd = YumaTokens.SegmentInner,
            bottomStart = YumaTokens.SegmentOuter, bottomEnd = YumaTokens.SegmentOuter
        )
    }
}

data class YumaGlassColors(
    val background: Color,
    val border: Color,
    val onGlass: Color
)

@Composable
fun yumaGlassColors(): YumaGlassColors {
    val colors = TeleBoxTheme.colors
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return YumaGlassColors(
        background = if (isDark) colors.surface.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.66f),
        border = colors.primary.copy(alpha = if (isDark) 0.16f else 0.20f),
        onGlass = scheme.onSurface
    )
}

/** Hairline gradient border: top->bottom light falloff prevents blinding joint seams. */
fun Modifier.glassBorder(
    shape: Shape,
    strokeWidth: Dp = YumaTokens.GlassBorderThickness,
    topAlpha: Float = 0.18f,
    bottomAlpha: Float = 0.04f,
    baseColor: Color
): Modifier = this.border(
    width = strokeWidth,
    brush = Brush.verticalGradient(
        0.0f to baseColor.copy(alpha = topAlpha),
        1.0f to baseColor.copy(alpha = bottomAlpha)
    ),
    shape = shape
)

/**
 * The core Yuma glass surface. Always chain [Modifier.yumaClickable] BEFORE this so the
 * whole card scales as one unit on press (see SKILL.md §4.3).
 */
@Composable
fun Modifier.yumaGlassCard(
    shape: Shape = RoundedCornerShape(YumaTokens.CardRadius),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    strokeWidth: Dp = YumaTokens.GlassBorderThickness,
    position: YumaSegmentPosition = YumaSegmentPosition.Single,
    topAlpha: Float? = null,
    bottomAlpha: Float? = null
): Modifier {
    val glass = yumaGlassColors()
    val bg = backgroundColor ?: glass.background
    val bc = borderColor ?: glass.border
    val alphas = yumaSegmentAlphas(position)
    val top = topAlpha ?: alphas.first
    val bot = bottomAlpha ?: alphas.second
    return this
        .clip(shape)
        .background(bg, shape)
        .glassBorder(shape, strokeWidth, top, bot, bc)
}

/**
 * Tactile press feedback: whole-element spring scale to [PressScale] with a light haptic tick.
 * Mirrors YumaPlayer's [yumaClickable] (SKILL.md §4.2).
 */
@Composable
fun Modifier.yumaClickable(
    enabled: Boolean = true,
    pressedScale: Float = YumaTokens.PressScale,
    haptic: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium),
        label = "yumaPress"
    )
    val view = LocalView.current
    return this
        .scale(scale, scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                if (haptic) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
        )
}

/** Convenience glass card composable with optional whole-card click handling. */
@Composable
fun YumaGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(YumaTokens.CardRadius),
    position: YumaSegmentPosition = YumaSegmentPosition.Single,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.yumaClickable(onClick = onClick) else Modifier)
            .yumaGlassCard(shape = shape, position = position)
            .clip(shape),
        content = content
    )
}
