package com.abrarshakhi.bricks.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class PaperTextureStyle { None, Halftone, Hatching }

fun Modifier.paper(
    color: Color,
    ink: Color,
    texture: PaperTextureStyle,
): Modifier = background(color).paperTexture(ink, texture)

fun Modifier.paperTexture(ink: Color, texture: PaperTextureStyle): Modifier = when (texture) {
    PaperTextureStyle.None -> this
    PaperTextureStyle.Halftone -> halftone(color = ink, dotRadius = 1.1.dp, spacing = 10.dp, alpha = 0.08f)
    PaperTextureStyle.Hatching -> hatching(color = ink, strokeWidth = 1.dp, spacing = 14.dp, alpha = 0.06f)
}

fun Modifier.halftone(
    color: Color,
    dotRadius: Dp = 1.5.dp,
    spacing: Dp = 6.dp,
    alpha: Float = 0.22f,
): Modifier = drawWithCache {
    val brush = halftoneBrush(color, dotRadius.toPx(), spacing.toPx())
    onDrawBehind {
        if (brush != null) drawRect(brush = brush, alpha = alpha)
    }
}

fun Modifier.hatching(
    color: Color,
    strokeWidth: Dp = 2.dp,
    spacing: Dp = 8.dp,
    alpha: Float = 0.3f,
): Modifier = drawWithCache {
    val brush = hatchingBrush(color, strokeWidth.toPx(), spacing.toPx())
    onDrawBehind {
        if (brush != null) drawRect(brush = brush, alpha = alpha)
    }
}

private fun halftoneBrush(color: Color, radius: Float, spacing: Float): ShaderBrush? {
    val step = spacing.roundToInt()
    if (step < 2 || radius <= 0f) return null
    val size = step.toFloat()
    val tile = ImageBitmap(step, step * 2)
    val canvas = Canvas(tile)
    val paint = Paint().apply { this.color = color }
    for (row in -1..3) {
        var x = if (row.mod(2) == 0) -size else -size / 2f
        while (x <= size * 2f) {
            canvas.drawCircle(Offset(x, row * size), radius, paint)
            x += size
        }
    }
    return ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
}

private fun hatchingBrush(color: Color, stroke: Float, spacing: Float): ShaderBrush? {
    val step = spacing.roundToInt()
    if (step < 2 || stroke <= 0f) return null
    val size = step.toFloat()
    val tile = ImageBitmap(step, step)
    val canvas = Canvas(tile)
    val paint = Paint().apply {
        this.color = color
        style = PaintingStyle.Stroke
        strokeWidth = stroke
    }
    for (line in -1..2) {
        val intercept = line * size
        canvas.drawLine(Offset(intercept + size, -size), Offset(intercept - size * 2f, size * 2f), paint)
    }
    return ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
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
