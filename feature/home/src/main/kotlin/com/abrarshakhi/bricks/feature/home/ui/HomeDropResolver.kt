package com.abrarshakhi.bricks.feature.home.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.ui.dragdrop.DragPayload
import com.abrarshakhi.bricks.core.ui.dragdrop.containsRoot
import com.abrarshakhi.bricks.core.ui.dragdrop.rootToLocal
import com.abrarshakhi.bricks.feature.home.HomeState
import com.abrarshakhi.bricks.feature.home.HomeTile
import kotlin.math.floor
import kotlin.math.roundToInt

@Immutable
internal sealed interface HomeDropTarget {
    data class Cell(val page: Int, val position: CellPosition) : HomeDropTarget
    data class Dock(val index: Int) : HomeDropTarget
    data class Merge(val tileId: Long) : HomeDropTarget
    data object Remove : HomeDropTarget
    data object Uninstall : HomeDropTarget
    data object None : HomeDropTarget
}

@Stable
internal class HomeDropResolver {
    var pager: LayoutCoordinates? = null
    var dock: LayoutCoordinates? = null
    var removeZone: LayoutCoordinates? = null
    var uninstallZone: LayoutCoordinates? = null

    fun resolve(
        rootPosition: Offset,
        payload: DragPayload,
        grabOffset: Offset,
        state: HomeState,
        page: Int,
        dockVertical: Boolean,
    ): HomeDropTarget {
        if (rootPosition == Offset.Unspecified) return HomeDropTarget.None
        val draggedId = (payload as? DragPayload.HomeItem)?.itemId
        val dragged = draggedId?.let(state::findTile)
        val appLike = payload is DragPayload.DrawerApp || dragged is HomeTile.App || dragged is HomeTile.Shortcut
        removeZone?.takeIf { it.containsRoot(rootPosition) }?.let {
            return if (payload is DragPayload.HomeItem) HomeDropTarget.Remove else HomeDropTarget.None
        }
        uninstallZone?.takeIf { it.containsRoot(rootPosition) }?.let {
            return if (payload is DragPayload.DrawerApp || dragged is HomeTile.App) HomeDropTarget.Uninstall else HomeDropTarget.None
        }
        dock?.takeIf { state.home.dockEnabled && it.containsRoot(rootPosition) }?.let { coordinates ->
            if (dragged is HomeTile.Widget) return HomeDropTarget.None
            return resolveDock(coordinates, rootPosition, state, draggedId, appLike, dockVertical)
        }
        pager?.takeIf { it.containsRoot(rootPosition) }?.let { coordinates ->
            return resolveGrid(coordinates, rootPosition, grabOffset, state, page, dragged, draggedId, appLike)
        }
        return HomeDropTarget.None
    }

    private fun resolveDock(
        coordinates: LayoutCoordinates,
        rootPosition: Offset,
        state: HomeState,
        draggedId: Long?,
        appLike: Boolean,
        vertical: Boolean,
    ): HomeDropTarget {
        val local = coordinates.rootToLocal(rootPosition)
        val length = if (vertical) coordinates.size.height.toFloat() else coordinates.size.width.toFloat()
        val along = if (vertical) local.y else local.x
        val items = state.dock
        val slots = state.home.dockSlots.coerceAtLeast(1)
        val slotLength = length / slots
        val start = (length - items.size * slotLength) / 2f
        val position = (along - start) / slotLength
        val slot = floor(position).toInt()
        val fraction = position - slot
        val occupant = items.getOrNull(slot)
        if (appLike && occupant != null && occupant.id != draggedId && fraction in MERGE_START..MERGE_END &&
            occupant !is HomeTile.Widget
        ) {
            return HomeDropTarget.Merge(occupant.id)
        }
        val boundary = position.roundToInt().coerceIn(0, items.size)
        val originalIndex = items.indexOfFirst { it.id == draggedId }
        val index = if (originalIndex >= 0 && boundary > originalIndex) boundary - 1 else boundary
        return HomeDropTarget.Dock(index)
    }

    private fun resolveGrid(
        coordinates: LayoutCoordinates,
        rootPosition: Offset,
        grabOffset: Offset,
        state: HomeState,
        page: Int,
        dragged: HomeTile?,
        draggedId: Long?,
        appLike: Boolean,
    ): HomeDropTarget {
        val local = coordinates.rootToLocal(rootPosition)
        val columns = state.grid.columns
        val rows = state.grid.rows
        val cellWidth = coordinates.size.width.toFloat() / columns
        val cellHeight = coordinates.size.height.toFloat() / rows
        if (cellWidth <= 0f || cellHeight <= 0f) return HomeDropTarget.None
        val span = (dragged?.placement as? Placement.Desktop)?.span?.takeIf { dragged is HomeTile.Widget } ?: GridSpan.Single
        val pointerX = floor(local.x / cellWidth).toInt()
        val pointerY = floor(local.y / cellHeight).toInt()
        if (appLike) {
            val tiles = state.pages.getOrNull(page)?.tiles.orEmpty()
            val occupant = tiles.firstOrNull { tile ->
                val placement = tile.placement as? Placement.Desktop ?: return@firstOrNull false
                tile.id != draggedId &&
                    pointerX in placement.cell.x until placement.cell.x + placement.span.width &&
                    pointerY in placement.cell.y until placement.cell.y + placement.span.height
            }
            if (occupant != null && occupant !is HomeTile.Widget) {
                val fx = local.x / cellWidth - pointerX
                val fy = local.y / cellHeight - pointerY
                if (fx in MERGE_START..MERGE_END && fy in MERGE_TOP..MERGE_BOTTOM) {
                    return HomeDropTarget.Merge(occupant.id)
                }
            }
        }
        val anchorX: Int
        val anchorY: Int
        if (dragged is HomeTile.Widget) {
            val topLeft = local - grabOffset
            anchorX = (topLeft.x / cellWidth).roundToInt()
            anchorY = (topLeft.y / cellHeight).roundToInt()
        } else {
            anchorX = pointerX
            anchorY = pointerY
        }
        return HomeDropTarget.Cell(
            page = page,
            position = CellPosition(
                x = anchorX.coerceIn(0, (columns - span.width).coerceAtLeast(0)),
                y = anchorY.coerceIn(0, (rows - span.height).coerceAtLeast(0)),
                spanX = span.width,
                spanY = span.height,
            ),
        )
    }

    private companion object {
        const val MERGE_START = 0.22f
        const val MERGE_END = 0.78f
        const val MERGE_TOP = 0.12f
        const val MERGE_BOTTOM = 0.72f
    }
}
