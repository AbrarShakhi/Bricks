package com.abrarshakhi.bricks.core.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.component.rememberWallpaperTextStyle
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.domain.model.IconStyle
import com.abrarshakhi.bricks.core.ui.icon.IconKind
import com.abrarshakhi.bricks.core.ui.icon.LoadedIcon
import com.abrarshakhi.paperui.PuiBadge
import com.abrarshakhi.paperui.PuiBadgedBox
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme
import com.abrarshakhi.paperui.puiHalftone
import com.abrarshakhi.paperui.puiHardShadow

val AppIconShape: Shape = RoundedCornerShape(percent = 24)

@Composable
fun InkTile(
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    shape: Shape = PuiTheme.shapes.medium,
    color: Color = PuiTheme.colors.surface,
    borderColor: Color = PuiTheme.colors.ink,
    borderWidth: Dp = PuiTheme.stroke.regular,
    elevation: Dp = PuiTheme.elevation.level2,
    content: @Composable BoxScope.() -> Unit,
) {
    val depth by animateDpAsState(
        targetValue = if (pressed) elevation else 0.dp,
        animationSpec = PuiTheme.motion.spec(PuiTheme.motion.instant),
        label = "InkTileDepth",
    )
    Box(
        modifier = modifier
            .offset { IntOffset(depth.roundToPx(), depth.roundToPx()) }
            .puiHardShadow(offset = elevation - depth, color = borderColor, shape = shape)
            .background(color = color, shape = shape)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .clip(shape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun AppIconTile(
    icon: LoadedIcon?,
    style: IconStyle,
    size: Dp,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    dimmed: Boolean = false,
) {
    val dimFilter = remember(dimmed) { if (dimmed) grayscaleFilter(contrast = 1f) else null }
    if (style == IconStyle.Classic) {
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.88f else 1f,
            animationSpec = PuiTheme.motion.spec(PuiTheme.motion.instant),
            label = "ClassicIconScale",
        )
        Box(
            modifier = modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (dimmed) 0.6f else 1f
                },
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon.bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = dimFilter,
                )
            } else {
                IconPlaceholder(Modifier.fillMaxSize().clip(AppIconShape))
            }
        }
        return
    }
    val ink = PuiTheme.colors.ink
    val inkStyle = style == IconStyle.Ink
    InkTile(
        modifier = modifier
            .size(size)
            .graphicsLayer { alpha = if (dimmed) 0.65f else 1f },
        pressed = pressed,
        shape = AppIconShape,
        color = PuiTheme.colors.surface,
        elevation = if (size >= 44.dp) PuiTheme.elevation.level2 else PuiTheme.elevation.level1,
    ) {
        when (icon?.kind) {
            IconKind.FullBleed, IconKind.System -> Image(
                bitmap = icon.bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = if (inkStyle) InkFilter else dimFilter,
            )
            IconKind.Monochrome -> Image(
                bitmap = icon.bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.82f),
                colorFilter = ColorFilter.tint(ink),
            )
            IconKind.Legacy -> Image(
                bitmap = icon.bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.8f),
                colorFilter = if (inkStyle) InkFilter else dimFilter,
            )
            null -> IconPlaceholder(Modifier.fillMaxSize())
        }
    }
}

@Composable
fun AppIconBadges(
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    isWork: Boolean = false,
    content: @Composable () -> Unit,
) {
    PuiBadgedBox(
        modifier = modifier,
        badge = {
            if (badgeCount > 0) {
                PuiBadge(color = PuiTheme.colors.primary, contentColor = PuiTheme.colors.onPrimary) {
                    PuiText(if (badgeCount > MAX_BADGE) "$MAX_BADGE+" else badgeCount.toString())
                }
            }
        },
    ) {
        Box {
            content()
            if (isWork) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(20.dp)
                        .background(PuiTheme.colors.accent, PuiTheme.shapes.pill)
                        .border(PuiTheme.stroke.hairline, PuiTheme.colors.ink, PuiTheme.shapes.pill),
                    contentAlignment = Alignment.Center,
                ) {
                    PuiIcon(BricksIcons.Work, contentDescription = null, size = 12.dp, tint = PuiTheme.colors.onAccent)
                }
            }
        }
    }
}

@Composable
fun AppLabel(
    text: String,
    modifier: Modifier = Modifier,
    onWallpaper: Boolean = false,
    maxLines: Int = 1,
    style: TextStyle = PuiTheme.typography.labelMedium,
) {
    val resolved = if (onWallpaper) rememberWallpaperTextStyle(style) else style
    PuiText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = resolved,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun IconPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier
            .background(PuiTheme.colors.surfaceMuted)
            .puiHalftone(color = PuiTheme.colors.ink, dotRadius = 1.dp, spacing = 5.dp, alpha = 0.2f),
    )
}

private const val MAX_BADGE = 99

private val InkFilter: ColorFilter = grayscaleFilter(contrast = 1.35f)

private fun grayscaleFilter(contrast: Float): ColorFilter {
    val matrix = ColorMatrix().apply { setToSaturation(0f) }
    if (contrast != 1f) {
        val translate = (1f - contrast) * 128f
        val scale = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        matrix.timesAssign(scale)
    }
    return ColorFilter.colorMatrix(matrix)
}
