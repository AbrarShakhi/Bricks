package com.abrarshakhi.bricks.feature.home.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.abrarshakhi.bricks.core.domain.model.GridSize

@Immutable
internal data class ScreenSpec(
    val width: Dp,
    val height: Dp,
) {
    val isLandscapePhone: Boolean
        get() = width > height && height < 480.dp

    val isTablet: Boolean
        get() = min(width, height) >= 600.dp

    val sidePadding: Dp
        get() = when {
            isTablet -> 32.dp
            isLandscapePhone -> 16.dp
            else -> 12.dp
        }

    val maxContentWidth: Dp
        get() = if (isTablet) 1100.dp else width
}

@Immutable
internal data class GridSpec(
    val grid: GridSize,
    val cellWidth: Dp,
    val cellHeight: Dp,
    val iconSize: Dp,
    val showLabels: Boolean,
) {
    val columns: Int
        get() = grid.columns

    val rows: Int
        get() = grid.rows
}

internal fun gridSpec(
    width: Dp,
    height: Dp,
    grid: GridSize,
    iconScale: Float,
    labelsEnabled: Boolean,
): GridSpec {
    val cellWidth = width / grid.columns
    val cellHeight = height / grid.rows
    val labels = labelsEnabled && cellHeight >= MIN_CELL_HEIGHT_WITH_LABEL
    val verticalRoom = cellHeight - if (labels) LABEL_SPACE else 10.dp
    val base = min(cellWidth * 0.68f, verticalRoom * 0.9f)
    val icon = (base * iconScale).coerceIn(MIN_ICON, MAX_ICON)
    return GridSpec(
        grid = grid,
        cellWidth = cellWidth,
        cellHeight = cellHeight,
        iconSize = min(icon, max(MIN_ICON, verticalRoom)),
        showLabels = labels,
    )
}

internal fun dockIconSize(availableLength: Dp, slots: Int, iconScale: Float, cap: Dp): Dp {
    val perSlot = availableLength / slots.coerceAtLeast(1)
    return (min(perSlot * 0.66f, cap) * iconScale).coerceIn(MIN_ICON, MAX_ICON)
}

private val MIN_CELL_HEIGHT_WITH_LABEL = 60.dp
private val LABEL_SPACE = 26.dp
private val MIN_ICON = 36.dp
private val MAX_ICON = 80.dp
