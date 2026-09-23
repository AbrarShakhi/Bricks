package com.abrarshakhi.bricks.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abrarshakhi.paperui.puiHalftone
import com.abrarshakhi.paperui.puiHatching

enum class PaperTextureStyle { None, Halftone, Hatching }

fun Modifier.paper(
    color: Color,
    ink: Color,
    texture: PaperTextureStyle,
): Modifier = background(color).paperTexture(ink, texture)

fun Modifier.paperTexture(ink: Color, texture: PaperTextureStyle): Modifier = when (texture) {
    PaperTextureStyle.None -> this
    PaperTextureStyle.Halftone -> puiHalftone(color = ink, dotRadius = 1.1.dp, spacing = 10.dp, alpha = 0.08f)
    PaperTextureStyle.Hatching -> puiHatching(color = ink, strokeWidth = 1.dp, spacing = 14.dp, alpha = 0.06f)
}

object WallpaperInk {
    val Light = Color(0xFFFFF8E7)
    val Dark = Color(0xFF1A1614)
}

@Composable
fun rememberWallpaperTextStyle(
    base: TextStyle,
    lightText: Boolean = true,
    shadowOffset: Dp = 1.5.dp,
): TextStyle {
    val density = LocalDensity.current
    return remember(base, lightText, shadowOffset, density) {
        val offset = with(density) { shadowOffset.toPx() }
        base.copy(
            color = if (lightText) WallpaperInk.Light else WallpaperInk.Dark,
            shadow = Shadow(
                color = if (lightText) WallpaperInk.Dark else WallpaperInk.Light,
                offset = Offset(offset, offset),
                blurRadius = 0f,
            ),
        )
    }
}
