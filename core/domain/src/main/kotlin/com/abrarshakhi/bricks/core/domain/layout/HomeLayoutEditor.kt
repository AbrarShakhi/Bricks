package com.abrarshakhi.bricks.core.domain.layout

import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.GridCell
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.HomeItem
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.model.canLiveInDock
import com.abrarshakhi.bricks.core.domain.model.canLiveInFolder

object HomeLayoutEditor {

    const val MAX_PAGES = 12

    fun addToDesktop(
        layout: HomeLayout,
        grid: GridSize,
        span: GridSpan,
        preferredPage: Int,
        create: (id: Long, placement: Placement) -> HomeItem,
    ): HomeLayout {
        val fitted = span.fittedTo(grid)
        val start = preferredPage.coerceIn(0, layout.pageCount - 1)
        val pageOrder = (start until layout.pageCount) + (0 until start)
        for (page in pageOrder) {
            val cell = GridOccupancy.of(layout, page, grid).firstFree(fitted) ?: continue
            return layout.copy(items = layout.items + create(layout.nextId, Placement.Desktop(page, cell, fitted)))
        }
        return layout.copy(
            pageCount = layout.pageCount + 1,
            items = layout.items + create(
                layout.nextId,
                Placement.Desktop(layout.pageCount, GridCell(0, 0), fitted),
            ),
        )
    }

    fun addToDesktopAt(
        layout: HomeLayout,
        grid: GridSize,
        span: GridSpan,
        page: Int,
        cell: GridCell,
        create: (id: Long, placement: Placement) -> HomeItem,
    ): HomeLayout {
        val fitted = span.fittedTo(grid)
        val targetPage = page.coerceIn(0, layout.pageCount)
        if (targetPage == layout.pageCount && targetPage >= MAX_PAGES) {
            return addToDesktop(layout, grid, fitted, layout.pageCount - 1, create)
        }
        val working = if (targetPage == layout.pageCount) layout.copy(pageCount = layout.pageCount + 1) else layout
        val anchor = GridCell(
            x = cell.x.coerceIn(0, grid.columns - fitted.width),
            y = cell.y.coerceIn(0, grid.rows - fitted.height),
        )
        val occupancy = GridOccupancy.of(working, targetPage, grid)
        val destination = if (occupancy.isFree(anchor, fitted)) anchor else occupancy.nearestFree(anchor, fitted)
        return if (destination != null) {
            working.copy(items = working.items + create(working.nextId, Placement.Desktop(targetPage, destination, fitted)))
        } else {
            addToDesktop(layout, grid, fitted, page, create)
        }
    }

    fun addToDock(
        layout: HomeLayout,
        dockSlots: Int,
        index: Int?,
        create: (id: Long, placement: Placement) -> HomeItem,
    ): HomeLayout? {
        val order = layout.dockItems.map { it.id }.toMutableList()
        if (order.size >= dockSlots) return null
        val item = create(layout.nextId, Placement.Dock(0))
        order.add((index ?: order.size).coerceIn(0, order.size), item.id)
        return layout.copy(items = layout.items + item).withDockOrder(order)
    }

    fun moveToDesktop(
        layout: HomeLayout,
        grid: GridSize,
        itemId: Long,
        page: Int,
        cell: GridCell,
    ): HomeLayout {
        val item = layout.find(itemId) ?: return layout
        val span = ((item.placement as? Placement.Desktop)?.span ?: GridSpan.Single).fittedTo(grid)
        val detached = detach(layout, item)
        val targetPage = page.coerceIn(0, detached.pageCount)
        if (targetPage == detached.pageCount && targetPage >= MAX_PAGES) return layout
        val working = if (targetPage == detached.pageCount) {
            detached.copy(pageCount = detached.pageCount + 1)
        } else {
            detached
        }
        val anchor = GridCell(
            x = cell.x.coerceIn(0, grid.columns - span.width),
            y = cell.y.coerceIn(0, grid.rows - span.height),
        )
        val occupancy = GridOccupancy.of(working, targetPage, grid)
        val destination = if (occupancy.isFree(anchor, span)) {
            anchor
        } else {
            occupancy.nearestFree(anchor, span)
        } ?: return layout
        return working.copy(
            items = working.items + item.withPlacement(Placement.Desktop(targetPage, destination, span)),
        )
    }

    fun moveToDock(
        layout: HomeLayout,
        itemId: Long,
        index: Int,
        dockSlots: Int,
    ): HomeLayout {
        val item = layout.find(itemId) ?: return layout
        if (!item.canLiveInDock) return layout
        val source = item.placement
        val detached = detach(layout, item)
        val order = detached.dockItems.map { it.id }.toMutableList()
        if (order.size < dockSlots) {
            order.add(index.coerceIn(0, order.size), item.id)
            return detached.copy(items = detached.items + item.withPlacement(Placement.Dock(0)))
                .withDockOrder(order)
        }
        if (source !is Placement.Desktop || source.span != GridSpan.Single || order.isEmpty()) return layout
        val targetIndex = index.coerceIn(0, order.lastIndex)
        val displacedId = order[targetIndex]
        order[targetIndex] = item.id
        val items = detached.items.map { if (it.id == displacedId) it.withPlacement(source) else it } +
            item.withPlacement(Placement.Dock(targetIndex))
        return detached.copy(items = items).withDockOrder(order)
    }

    fun mergeIntoFolder(
        layout: HomeLayout,
        targetId: Long,
        draggedId: Long,
        defaultTitle: String,
    ): HomeLayout {
        if (targetId == draggedId) return layout
        val target = layout.find(targetId) ?: return layout
        val dragged = layout.find(draggedId) ?: return layout
        if (!dragged.canLiveInFolder) return layout
        return when (target) {
            is HomeItem.Folder -> {
                val draggedPlacement = dragged.placement
                if (draggedPlacement is Placement.InFolder && draggedPlacement.folderId == target.id) return layout
                val detached = detach(layout, dragged)
                if (detached.find(target.id) == null) return layout
                val rank = detached.folderContents(target.id).size
                detached.copy(items = detached.items + dragged.withPlacement(Placement.InFolder(target.id, rank)))
            }

            is HomeItem.Application, is HomeItem.Shortcut -> {
                if (target.placement is Placement.InFolder) return layout
                val folderId = layout.nextId
                val detached = detach(layout, dragged)
                val anchor = detached.find(targetId) ?: return layout
                if (anchor.placement is Placement.InFolder) return layout
                val folder = HomeItem.Folder(id = folderId, placement = anchor.placement, title = defaultTitle)
                val items = detached.items.map {
                    if (it.id == targetId) it.withPlacement(Placement.InFolder(folderId, 0)) else it
                } + folder + dragged.withPlacement(Placement.InFolder(folderId, 1))
                detached.copy(items = items)
            }

            is HomeItem.Widget -> layout
        }
    }

    fun moveWithinFolder(layout: HomeLayout, itemId: Long, rank: Int): HomeLayout {
        val item = layout.find(itemId) ?: return layout
        val placement = item.placement as? Placement.InFolder ?: return layout
        val order = layout.folderContents(placement.folderId).map { it.id }.toMutableList()
        order.remove(itemId)
        order.add(rank.coerceIn(0, order.size), itemId)
        return layout.copy(
            items = layout.items.map {
                val index = order.indexOf(it.id)
                if (index >= 0) it.withPlacement(Placement.InFolder(placement.folderId, index)) else it
            },
        )
    }

    fun moveOutOfFolder(
        layout: HomeLayout,
        grid: GridSize,
        itemId: Long,
        preferredPage: Int,
    ): HomeLayout {
        val item = layout.find(itemId) ?: return layout
        if (item.placement !is Placement.InFolder) return layout
        val detached = detach(layout, item)
        return addToDesktop(detached, grid, GridSpan.Single, preferredPage) { _, placement ->
            item.withPlacement(placement)
        }
    }

    fun remove(layout: HomeLayout, itemId: Long): HomeLayout {
        val item = layout.find(itemId) ?: return layout
        val withoutChildren = if (item is HomeItem.Folder) {
            layout.copy(items = layout.items.filterNot { (it.placement as? Placement.InFolder)?.folderId == item.id })
        } else {
            layout
        }
        return detach(withoutChildren, item)
    }

    fun renameFolder(layout: HomeLayout, folderId: Long, title: String): HomeLayout = layout.copy(
        items = layout.items.map {
            if (it.id == folderId && it is HomeItem.Folder) it.copy(title = title) else it
        },
    )

    fun resizeWidget(layout: HomeLayout, grid: GridSize, itemId: Long, span: GridSpan): HomeLayout {
        val item = layout.find(itemId) as? HomeItem.Widget ?: return layout
        val placement = item.placement as? Placement.Desktop ?: return layout
        val fitted = span.fittedTo(grid)
        val anchor = GridCell(
            x = placement.cell.x.coerceAtMost(grid.columns - fitted.width),
            y = placement.cell.y.coerceAtMost(grid.rows - fitted.height),
        )
        val occupancy = GridOccupancy.of(layout, placement.page, grid, excluding = itemId)
        if (!occupancy.isFree(anchor, fitted)) return layout
        return layout.replace(item.withPlacement(placement.copy(cell = anchor, span = fitted)))
    }

    fun addPage(layout: HomeLayout): HomeLayout =
        if (layout.pageCount >= MAX_PAGES) layout else layout.copy(pageCount = layout.pageCount + 1)

    fun removePage(layout: HomeLayout, page: Int): HomeLayout {
        if (layout.pageCount <= 1 || page !in 0 until layout.pageCount) return layout
        val onPage = layout.desktopItems(page)
        val removedIds = onPage.map { it.id }.toSet()
        val removedFolders = onPage.filterIsInstance<HomeItem.Folder>().map { it.id }.toSet()
        val items = layout.items
            .filterNot { it.id in removedIds || (it.placement as? Placement.InFolder)?.folderId in removedFolders }
            .map { item ->
                val placement = item.placement
                if (placement is Placement.Desktop && placement.page > page) {
                    item.withPlacement(placement.copy(page = placement.page - 1))
                } else {
                    item
                }
            }
        return layout.copy(pageCount = layout.pageCount - 1, items = items)
    }

    fun removeEmptyPages(layout: HomeLayout): HomeLayout {
        val usedPages = layout.items.mapNotNull { (it.placement as? Placement.Desktop)?.page }.toSortedSet()
        if (usedPages.size == layout.pageCount) return layout
        if (usedPages.isEmpty()) return layout.copy(pageCount = 1)
        val remap = usedPages.withIndex().associate { (index, page) -> page to index }
        return layout.copy(
            pageCount = usedPages.size,
            items = layout.items.map { item ->
                val placement = item.placement
                if (placement is Placement.Desktop) {
                    item.withPlacement(placement.copy(page = remap.getValue(placement.page)))
                } else {
                    item
                }
            },
        )
    }

    fun reflow(layout: HomeLayout, grid: GridSize, dockSlots: Int): HomeLayout {
        val misplaced = layout.items.filter { item ->
            val placement = item.placement as? Placement.Desktop ?: return@filter false
            placement.page !in 0 until layout.pageCount ||
                placement.span != placement.span.fittedTo(grid) ||
                !grid.contains(placement.cell, placement.span)
        }.sortedWith(desktopOrder)
        val dockOverflow = layout.dockItems.drop(dockSlots.coerceAtLeast(0))
        if (misplaced.isEmpty() && dockOverflow.isEmpty() && !hasOverlaps(layout, grid)) return layout

        val displaced = misplaced + dockOverflow + overlapping(layout, grid, misplaced.map { it.id }.toSet())
        val displacedIds = displaced.map { it.id }.toSet()
        var result = layout.copy(items = layout.items.filterNot { it.id in displacedIds })
            .let { it.withDockOrder(it.dockItems.map { item -> item.id }) }
        displaced.forEach { item ->
            val placement = item.placement
            val span = ((placement as? Placement.Desktop)?.span ?: GridSpan.Single).fittedTo(grid)
            val page = (placement as? Placement.Desktop)?.page?.coerceIn(0, result.pageCount - 1) ?: 0
            result = addToDesktop(result, grid, span, page) { _, newPlacement -> item.withPlacement(newPlacement) }
        }
        return result
    }

    fun removeMissing(
        layout: HomeLayout,
        keepApp: (AppKey) -> Boolean,
        keepShortcut: (ShortcutKey) -> Boolean,
    ): HomeLayout {
        val stale = layout.items.filter {
            (it is HomeItem.Application && !keepApp(it.app)) ||
                (it is HomeItem.Shortcut && !keepShortcut(it.shortcut))
        }
        return stale.fold(layout) { acc, item -> remove(acc, item.id) }
    }

    fun containsApp(layout: HomeLayout, app: AppKey): Boolean =
        layout.items.any { it is HomeItem.Application && it.app == app }

    fun widgetIds(layout: HomeLayout): Set<Int> =
        layout.items.filterIsInstance<HomeItem.Widget>().map { it.appWidgetId }.toSet()

    private fun detach(layout: HomeLayout, item: HomeItem): HomeLayout {
        val remaining = layout.copy(items = layout.items.filterNot { it.id == item.id })
        return when (val placement = item.placement) {
            is Placement.Desktop -> remaining
            is Placement.Dock -> remaining.withDockOrder(remaining.dockItems.map { it.id })
            is Placement.InFolder -> cleanupFolder(remaining, placement.folderId)
        }
    }

    private fun cleanupFolder(layout: HomeLayout, folderId: Long): HomeLayout {
        val folder = layout.find(folderId)
        val children = layout.folderContents(folderId)
        if (folder == null) {
            val orphanIds = children.map { it.id }.toSet()
            return layout.copy(items = layout.items.filterNot { it.id in orphanIds })
        }
        return when (children.size) {
            0 -> layout.copy(items = layout.items.filterNot { it.id == folderId })
            1 -> {
                val child = children.single()
                layout.copy(
                    items = layout.items.mapNotNull {
                        when (it.id) {
                            folderId -> null
                            child.id -> child.withPlacement(folder.placement)
                            else -> it
                        }
                    },
                )
            }

            else -> {
                val order = children.map { it.id }
                layout.copy(
                    items = layout.items.map {
                        val index = order.indexOf(it.id)
                        if (index >= 0) it.withPlacement(Placement.InFolder(folderId, index)) else it
                    },
                )
            }
        }
    }

    private fun hasOverlaps(layout: HomeLayout, grid: GridSize): Boolean =
        (0 until layout.pageCount).any { page -> overlappingOnPage(layout, grid, page, emptySet()).isNotEmpty() }

    private fun overlapping(layout: HomeLayout, grid: GridSize, ignored: Set<Long>): List<HomeItem> =
        (0 until layout.pageCount).flatMap { page -> overlappingOnPage(layout, grid, page, ignored) }

    private fun overlappingOnPage(
        layout: HomeLayout,
        grid: GridSize,
        page: Int,
        ignored: Set<Long>,
    ): List<HomeItem> {
        val occupancy = GridOccupancy(grid)
        val overlapping = mutableListOf<HomeItem>()
        layout.desktopItems(page)
            .filterNot { it.id in ignored }
            .sortedWith(desktopOrder)
            .forEach { item ->
                val placement = item.placement as Placement.Desktop
                if (occupancy.isFree(placement.cell, placement.span)) {
                    occupancy.occupy(item.id, placement.cell, placement.span)
                } else {
                    overlapping += item
                }
            }
        return overlapping
    }

    private fun HomeLayout.withDockOrder(order: List<Long>): HomeLayout = copy(
        items = items.map {
            val index = order.indexOf(it.id)
            if (index >= 0) it.withPlacement(Placement.Dock(index)) else it
        },
    )

    private fun HomeLayout.replace(item: HomeItem): HomeLayout =
        copy(items = items.map { if (it.id == item.id) item else it })

    private fun GridSpan.fittedTo(grid: GridSize): GridSpan = GridSpan(
        width = width.coerceIn(1, grid.columns),
        height = height.coerceIn(1, grid.rows),
    )

    private val desktopOrder = compareBy<HomeItem>(
        { (it.placement as Placement.Desktop).page },
        { (it.placement as Placement.Desktop).cell.y },
        { (it.placement as Placement.Desktop).cell.x },
    )
}
