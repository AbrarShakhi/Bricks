package com.abrarshakhi.bricks.core.domain.model

data class GridSize(
    val columns: Int,
    val rows: Int,
) {
    fun contains(cell: GridCell, span: GridSpan = GridSpan.Single): Boolean =
        cell.x >= 0 && cell.y >= 0 &&
            cell.x + span.width <= columns &&
            cell.y + span.height <= rows

    companion object {
        const val MIN_COLUMNS = 3
        const val MAX_COLUMNS = 10
        const val MIN_ROWS = 3
        const val MAX_ROWS = 10

        fun of(columns: Int, rows: Int): GridSize = GridSize(
            columns = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS),
            rows = rows.coerceIn(MIN_ROWS, MAX_ROWS),
        )

        fun recommendedFor(smallestWidthDp: Int, largestWidthDp: Int): GridSize {
            val columns = when {
                smallestWidthDp >= 840 -> 8
                smallestWidthDp >= 600 -> 6
                smallestWidthDp >= 400 -> 5
                else -> 4
            }
            val aspect = largestWidthDp.toFloat() / smallestWidthDp.coerceAtLeast(1)
            val rows = when {
                smallestWidthDp >= 600 -> 5
                aspect >= 2.1f -> 6
                aspect >= 1.8f -> 5
                else -> 4
            }
            return of(columns, rows)
        }
    }
}

data class GridCell(
    val x: Int,
    val y: Int,
)

data class GridSpan(
    val width: Int,
    val height: Int,
) {
    val area: Int
        get() = width * height

    companion object {
        val Single = GridSpan(1, 1)
    }
}
