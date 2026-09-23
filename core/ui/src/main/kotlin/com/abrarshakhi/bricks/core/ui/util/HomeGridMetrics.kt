package com.abrarshakhi.bricks.core.ui.util

import com.abrarshakhi.bricks.core.domain.model.GridSize
import java.util.concurrent.ConcurrentHashMap

data class CellSize(val widthDp: Float, val heightDp: Float)

class HomeGridMetrics {
    private val measured = ConcurrentHashMap<Key, CellSize>()

    fun update(grid: GridSize, landscape: Boolean, widthDp: Float, heightDp: Float) {
        if (widthDp > 0f && heightDp > 0f) measured[Key(grid, landscape)] = CellSize(widthDp, heightDp)
    }

    fun cellSize(grid: GridSize, screenWidthDp: Int, screenHeightDp: Int): CellSize =
        measured[Key(grid, screenWidthDp > screenHeightDp)] ?: CellSize(
            widthDp = (screenWidthDp - HORIZONTAL_PADDING_DP).coerceAtLeast(1f) / grid.columns,
            heightDp = screenHeightDp * GRID_HEIGHT_FRACTION / grid.rows,
        )

    private data class Key(val grid: GridSize, val landscape: Boolean)

    private companion object {
        const val HORIZONTAL_PADDING_DP = 24f
        const val GRID_HEIGHT_FRACTION = 0.62f
    }
}
