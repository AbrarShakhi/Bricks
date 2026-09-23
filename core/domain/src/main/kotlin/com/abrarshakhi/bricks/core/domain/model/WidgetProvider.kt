package com.abrarshakhi.bricks.core.domain.model

import kotlin.math.ceil
import kotlin.math.floor

data class WidgetProvider(
    val key: WidgetProviderKey,
    val label: String,
    val appLabel: String,
    val description: String?,
    val profile: ProfileType,
    val minWidthDp: Int,
    val minHeightDp: Int,
    val minResizeWidthDp: Int,
    val minResizeHeightDp: Int,
    val maxResizeWidthDp: Int?,
    val maxResizeHeightDp: Int?,
    val targetCellWidth: Int?,
    val targetCellHeight: Int?,
    val resizeHorizontal: Boolean,
    val resizeVertical: Boolean,
    val isConfigurable: Boolean,
) {
    fun defaultSpan(cellWidthDp: Float, cellHeightDp: Float, grid: GridSize): GridSpan {
        val width = targetCellWidth?.takeIf { it > 0 } ?: spanFor(minWidthDp, cellWidthDp)
        val height = targetCellHeight?.takeIf { it > 0 } ?: spanFor(minHeightDp, cellHeightDp)
        return GridSpan(
            width = width.coerceIn(1, grid.columns),
            height = height.coerceIn(1, grid.rows),
        )
    }

    fun minimumSpan(cellWidthDp: Float, cellHeightDp: Float, grid: GridSize): GridSpan {
        val default = defaultSpan(cellWidthDp, cellHeightDp, grid)
        val width = if (resizeHorizontal) {
            spanFor(minResizeWidthDp.takeIf { it > 0 } ?: minWidthDp, cellWidthDp)
        } else {
            default.width
        }
        val height = if (resizeVertical) {
            spanFor(minResizeHeightDp.takeIf { it > 0 } ?: minHeightDp, cellHeightDp)
        } else {
            default.height
        }
        return GridSpan(
            width = width.coerceIn(1, default.width),
            height = height.coerceIn(1, default.height),
        )
    }

    fun maximumSpan(cellWidthDp: Float, cellHeightDp: Float, grid: GridSize): GridSpan {
        val default = defaultSpan(cellWidthDp, cellHeightDp, grid)
        val width = when {
            !resizeHorizontal -> default.width
            maxResizeWidthDp != null && maxResizeWidthDp > 0 -> floorSpanFor(maxResizeWidthDp, cellWidthDp)
            else -> grid.columns
        }
        val height = when {
            !resizeVertical -> default.height
            maxResizeHeightDp != null && maxResizeHeightDp > 0 -> floorSpanFor(maxResizeHeightDp, cellHeightDp)
            else -> grid.rows
        }
        return GridSpan(
            width = width.coerceIn(default.width, grid.columns),
            height = height.coerceIn(default.height, grid.rows),
        )
    }

    private fun spanFor(sizeDp: Int, cellDp: Float): Int {
        if (cellDp <= 0f) return 1
        return ceil((sizeDp + CELL_PADDING_DP) / cellDp).toInt().coerceAtLeast(1)
    }

    private fun floorSpanFor(sizeDp: Int, cellDp: Float): Int {
        if (cellDp <= 0f) return 1
        return floor((sizeDp + CELL_PADDING_DP) / cellDp).toInt().coerceAtLeast(1)
    }

    private companion object {
        const val CELL_PADDING_DP = 16f
    }
}
