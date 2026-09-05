package com.telebox.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

enum class WindowWidthSize {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun rememberWindowWidthSize(): WindowWidthSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) {
        when {
            widthDp < 600 -> WindowWidthSize.COMPACT
            widthDp < 840 -> WindowWidthSize.MEDIUM
            else -> WindowWidthSize.EXPANDED
        }
    }
}

val WindowWidthSize.isCompact: Boolean
    get() = this == WindowWidthSize.COMPACT

val WindowWidthSize.useModalDrawer: Boolean
    get() = this != WindowWidthSize.EXPANDED
