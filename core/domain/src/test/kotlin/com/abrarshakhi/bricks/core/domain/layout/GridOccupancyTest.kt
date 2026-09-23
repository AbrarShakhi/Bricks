package com.abrarshakhi.bricks.core.domain.layout

import com.abrarshakhi.bricks.core.domain.model.GridCell
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridOccupancyTest {

    private val grid = GridSize(columns = 4, rows = 4)

    @Test
    fun `occupied cells are not free`() {
        val occupancy = GridOccupancy(grid)
        occupancy.occupy(1L, GridCell(1, 1), GridSpan(2, 2))
        assertFalse(occupancy.isFree(GridCell(2, 2), GridSpan.Single))
        assertTrue(occupancy.isFree(GridCell(2, 2), GridSpan.Single, ignoring = 1L))
        assertEquals(1L, occupancy.occupant(GridCell(1, 2)))
    }

    @Test
    fun `spans that leave the grid are never free`() {
        val occupancy = GridOccupancy(grid)
        assertFalse(occupancy.isFree(GridCell(3, 0), GridSpan(2, 1)))
        assertFalse(occupancy.isFree(GridCell(-1, 0), GridSpan.Single))
    }

    @Test
    fun `firstFree scans row by row`() {
        val occupancy = GridOccupancy(grid)
        occupancy.occupy(1L, GridCell(0, 0), GridSpan(4, 1))
        assertEquals(GridCell(0, 1), occupancy.firstFree(GridSpan.Single))
        assertEquals(GridCell(0, 1), occupancy.firstFree(GridSpan(4, 3)))
        assertNull(occupancy.firstFree(GridSpan(4, 4)))
    }

    @Test
    fun `nearestFree picks a neighbouring cell`() {
        val occupancy = GridOccupancy(grid)
        occupancy.occupy(1L, GridCell(2, 2), GridSpan.Single)
        val nearest = occupancy.nearestFree(GridCell(2, 2), GridSpan.Single)!!
        val distance = (nearest.x - 2) * (nearest.x - 2) + (nearest.y - 2) * (nearest.y - 2)
        assertEquals(1, distance)
    }

    @Test
    fun `free cell count reflects occupancy`() {
        val occupancy = GridOccupancy(grid)
        occupancy.occupy(1L, GridCell(0, 0), GridSpan(2, 2))
        assertEquals(12, occupancy.freeCellCount)
    }
}
