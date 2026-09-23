package com.abrarshakhi.bricks.core.domain.layout

import com.abrarshakhi.bricks.core.domain.model.GridCell
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.model.Placement

class GridOccupancy(val grid: GridSize) {

    private val occupants = Array(grid.rows) { LongArray(grid.columns) { NONE } }

    fun occupy(id: Long, cell: GridCell, span: GridSpan) {
        for (y in cell.y until cell.y + span.height) {
            for (x in cell.x until cell.x + span.width) {
                if (x in 0 until grid.columns && y in 0 until grid.rows) {
                    occupants[y][x] = id
                }
            }
        }
    }

    fun occupant(cell: GridCell): Long? {
        if (cell.x !in 0 until grid.columns || cell.y !in 0 until grid.rows) return null
        return occupants[cell.y][cell.x].takeIf { it != NONE }
    }

    fun isFree(cell: GridCell, span: GridSpan, ignoring: Long? = null): Boolean {
        if (!grid.contains(cell, span)) return false
        for (y in cell.y until cell.y + span.height) {
            for (x in cell.x until cell.x + span.width) {
                val id = occupants[y][x]
                if (id != NONE && id != ignoring) return false
            }
        }
        return true
    }

    fun firstFree(span: GridSpan): GridCell? {
        for (y in 0..grid.rows - span.height) {
            for (x in 0..grid.columns - span.width) {
                val cell = GridCell(x, y)
                if (isFree(cell, span)) return cell
            }
        }
        return null
    }

    fun nearestFree(target: GridCell, span: GridSpan, ignoring: Long? = null): GridCell? {
        var best: GridCell? = null
        var bestDistance = Int.MAX_VALUE
        for (y in 0..grid.rows - span.height) {
            for (x in 0..grid.columns - span.width) {
                val cell = GridCell(x, y)
                if (!isFree(cell, span, ignoring)) continue
                val dx = x - target.x
                val dy = y - target.y
                val distance = dx * dx + dy * dy
                if (distance < bestDistance) {
                    best = cell
                    bestDistance = distance
                }
            }
        }
        return best
    }

    val freeCellCount: Int
        get() = occupants.sumOf { row -> row.count { it == NONE } }

    companion object {
        private const val NONE = Long.MIN_VALUE

        fun of(layout: HomeLayout, page: Int, grid: GridSize, excluding: Long? = null): GridOccupancy {
            val occupancy = GridOccupancy(grid)
            layout.desktopItems(page).forEach { item ->
                if (item.id != excluding) {
                    val placement = item.placement as Placement.Desktop
                    occupancy.occupy(item.id, placement.cell, placement.span)
                }
            }
            return occupancy
        }
    }
}
