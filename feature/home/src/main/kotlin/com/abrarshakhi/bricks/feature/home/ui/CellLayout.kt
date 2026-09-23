package com.abrarshakhi.bricks.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

@Immutable
internal data class CellPosition(
    val x: Int,
    val y: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
)

internal fun Modifier.cell(position: CellPosition): Modifier = layoutId(position)

@Composable
internal fun CellLayout(
    columns: Int,
    rows: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val cellWidth = width.toFloat() / columns.coerceAtLeast(1)
        val cellHeight = height.toFloat() / rows.coerceAtLeast(1)
        val placeables = measurables.map { measurable ->
            val position = measurable.layoutId as? CellPosition ?: CellPosition(0, 0)
            val left = (position.x * cellWidth).roundToInt()
            val top = (position.y * cellHeight).roundToInt()
            val right = ((position.x + position.spanX) * cellWidth).roundToInt()
            val bottom = ((position.y + position.spanY) * cellHeight).roundToInt()
            val placeable = measurable.measure(
                Constraints.fixed((right - left).coerceAtLeast(0), (bottom - top).coerceAtLeast(0)),
            )
            Triple(placeable, left, top)
        }
        layout(width, height) {
            placeables.forEach { (placeable, x, y) -> placeable.place(x, y) }
        }
    }
}
