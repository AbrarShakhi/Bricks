package com.abrarshakhi.bricks.core.domain.layout

import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.GridCell
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.HomeItem
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutEditorTest {

    private val grid = GridSize(columns = 4, rows = 5)

    private fun app(name: String) = AppKey("pkg.$name", "$name.Main", 0L)

    private fun HomeLayout.addApp(name: String, page: Int = 0): HomeLayout =
        HomeLayoutEditor.addToDesktop(this, grid, GridSpan.Single, page) { id, placement ->
            HomeItem.Application(id, placement, app(name))
        }

    private fun HomeLayout.addDockApp(name: String, slots: Int = 4): HomeLayout =
        HomeLayoutEditor.addToDock(this, slots, null) { id, placement ->
            HomeItem.Application(id, placement, app(name))
        }!!

    private fun HomeLayout.placementOf(name: String): Placement =
        items.first { it is HomeItem.Application && it.app == app(name) }.placement

    private fun HomeLayout.idOf(name: String): Long =
        items.first { it is HomeItem.Application && it.app == app(name) }.id

    @Test
    fun `addToDesktop fills rows left to right`() {
        val layout = HomeLayout.Empty.addApp("a").addApp("b").addApp("c")
        assertEquals(Placement.Desktop(0, GridCell(0, 0)), layout.placementOf("a"))
        assertEquals(Placement.Desktop(0, GridCell(1, 0)), layout.placementOf("b"))
        assertEquals(Placement.Desktop(0, GridCell(2, 0)), layout.placementOf("c"))
    }

    @Test
    fun `addToDesktop appends a page when all pages are full`() {
        var layout = HomeLayout.Empty
        repeat(grid.columns * grid.rows) { layout = layout.addApp("app$it") }
        layout = layout.addApp("overflow")
        assertEquals(2, layout.pageCount)
        assertEquals(Placement.Desktop(1, GridCell(0, 0)), layout.placementOf("overflow"))
    }

    @Test
    fun `ids stay unique`() {
        val layout = HomeLayout.Empty.addApp("a").addApp("b").addDockApp("c")
        assertEquals(layout.items.size, layout.items.map { it.id }.toSet().size)
    }

    @Test
    fun `moveToDesktop onto an occupied cell lands on the nearest free cell`() {
        val layout = HomeLayout.Empty.addApp("a").addApp("b")
        val moved = HomeLayoutEditor.moveToDesktop(layout, grid, layout.idOf("b"), 0, GridCell(0, 0))
        val placement = moved.placementOf("b") as Placement.Desktop
        assertEquals(0, placement.page)
        assertTrue(placement.cell == GridCell(1, 0) || placement.cell == GridCell(0, 1))
        assertEquals(Placement.Desktop(0, GridCell(0, 0)), moved.placementOf("a"))
    }

    @Test
    fun `moveToDesktop onto the page after the last creates it`() {
        val layout = HomeLayout.Empty.addApp("a")
        val moved = HomeLayoutEditor.moveToDesktop(layout, grid, layout.idOf("a"), 1, GridCell(2, 3))
        assertEquals(2, moved.pageCount)
        assertEquals(Placement.Desktop(1, GridCell(2, 3)), moved.placementOf("a"))
    }

    @Test
    fun `moveToDock inserts and renumbers`() {
        val layout = HomeLayout.Empty.addDockApp("d0").addDockApp("d1").addApp("a")
        val moved = HomeLayoutEditor.moveToDock(layout, layout.idOf("a"), 1, dockSlots = 4)
        assertEquals(listOf(app("d0"), app("a"), app("d1")), moved.dockItems.map { (it as HomeItem.Application).app })
        assertEquals(listOf(0, 1, 2), moved.dockItems.map { (it.placement as Placement.Dock).index })
    }

    @Test
    fun `moveToDock swaps with the occupant when the dock is full`() {
        val layout = HomeLayout.Empty.addDockApp("d0", 2).addDockApp("d1", 2).addApp("a")
        val source = layout.placementOf("a")
        val moved = HomeLayoutEditor.moveToDock(layout, layout.idOf("a"), 0, dockSlots = 2)
        assertEquals(Placement.Dock(0), moved.placementOf("a"))
        assertEquals(source, moved.placementOf("d0"))
    }

    @Test
    fun `dragging within the dock reorders it`() {
        val layout = HomeLayout.Empty.addDockApp("d0").addDockApp("d1").addDockApp("d2")
        val moved = HomeLayoutEditor.moveToDock(layout, layout.idOf("d0"), 2, dockSlots = 4)
        assertEquals(listOf(app("d1"), app("d2"), app("d0")), moved.dockItems.map { (it as HomeItem.Application).app })
    }

    @Test
    fun `merging two apps creates a folder in the target's place`() {
        val layout = HomeLayout.Empty.addApp("a").addApp("b")
        val merged = HomeLayoutEditor.mergeIntoFolder(layout, layout.idOf("a"), layout.idOf("b"), "Folder")
        val folder = merged.items.filterIsInstance<HomeItem.Folder>().single()
        assertEquals(Placement.Desktop(0, GridCell(0, 0)), folder.placement)
        assertEquals(listOf(app("a"), app("b")), merged.folderContents(folder.id).map { (it as HomeItem.Application).app })
    }

    @Test
    fun `dropping on a folder appends to it`() {
        val base = HomeLayout.Empty.addApp("a").addApp("b").addApp("c")
        val merged = HomeLayoutEditor.mergeIntoFolder(base, base.idOf("a"), base.idOf("b"), "Folder")
        val folderId = merged.items.filterIsInstance<HomeItem.Folder>().single().id
        val added = HomeLayoutEditor.mergeIntoFolder(merged, folderId, merged.idOf("c"), "Folder")
        assertEquals(3, added.folderContents(folderId).size)
        assertEquals(Placement.InFolder(folderId, 2), added.placementOf("c"))
    }

    @Test
    fun `removing one of two folder children dissolves the folder`() {
        val base = HomeLayout.Empty.addApp("a").addApp("b")
        val merged = HomeLayoutEditor.mergeIntoFolder(base, base.idOf("a"), base.idOf("b"), "Folder")
        val removed = HomeLayoutEditor.remove(merged, merged.idOf("b"))
        assertTrue(removed.items.none { it is HomeItem.Folder })
        assertEquals(Placement.Desktop(0, GridCell(0, 0)), removed.placementOf("a"))
    }

    @Test
    fun `removing a folder removes its children`() {
        val base = HomeLayout.Empty.addApp("a").addApp("b")
        val merged = HomeLayoutEditor.mergeIntoFolder(base, base.idOf("a"), base.idOf("b"), "Folder")
        val folderId = merged.items.filterIsInstance<HomeItem.Folder>().single().id
        assertTrue(HomeLayoutEditor.remove(merged, folderId).items.isEmpty())
    }

    @Test
    fun `dropping a folder child onto its own folder is a no-op`() {
        val base = HomeLayout.Empty.addApp("a").addApp("b")
        val merged = HomeLayoutEditor.mergeIntoFolder(base, base.idOf("a"), base.idOf("b"), "Folder")
        val folderId = merged.items.filterIsInstance<HomeItem.Folder>().single().id
        assertSame(merged, HomeLayoutEditor.mergeIntoFolder(merged, folderId, merged.idOf("b"), "Folder"))
    }

    @Test
    fun `moving a child out of a folder places it on the desktop`() {
        val base = HomeLayout.Empty.addApp("a").addApp("b").addApp("c")
        val merged = HomeLayoutEditor.mergeIntoFolder(base, base.idOf("a"), base.idOf("b"), "Folder")
        val folderId = merged.items.filterIsInstance<HomeItem.Folder>().single().id
        val withC = HomeLayoutEditor.mergeIntoFolder(merged, folderId, merged.idOf("c"), "Folder")
        val out = HomeLayoutEditor.moveOutOfFolder(withC, grid, withC.idOf("c"), 0)
        assertTrue(out.placementOf("c") is Placement.Desktop)
        assertEquals(2, out.folderContents(folderId).size)
    }

    @Test
    fun `widgets cannot be merged into folders`() {
        val layout = HomeLayoutEditor.addToDesktop(HomeLayout.Empty, grid, GridSpan(2, 2), 0) { id, placement ->
            HomeItem.Widget(id, placement, 7, WidgetProviderKey("w", "W", 0))
        }.addApp("a")
        val widgetId = layout.items.filterIsInstance<HomeItem.Widget>().single().id
        assertSame(layout, HomeLayoutEditor.mergeIntoFolder(layout, layout.idOf("a"), widgetId, "Folder"))
    }

    @Test
    fun `resizeWidget refuses to overlap other items`() {
        val layout = HomeLayoutEditor.addToDesktop(HomeLayout.Empty, grid, GridSpan(2, 1), 0) { id, placement ->
            HomeItem.Widget(id, placement, 7, WidgetProviderKey("w", "W", 0))
        }.addApp("a")
        val widgetId = layout.items.filterIsInstance<HomeItem.Widget>().single().id
        assertSame(layout, HomeLayoutEditor.resizeWidget(layout, grid, widgetId, GridSpan(3, 1)))
        val taller = HomeLayoutEditor.resizeWidget(layout, grid, widgetId, GridSpan(2, 2))
        assertEquals(GridSpan(2, 2), (taller.find(widgetId)!!.placement as Placement.Desktop).span)
    }

    @Test
    fun `removePage drops its items and shifts later pages`() {
        val layout = HomeLayout(
            pageCount = 3,
            items = listOf(
                HomeItem.Application(1, Placement.Desktop(0, GridCell(0, 0)), app("p0")),
                HomeItem.Application(2, Placement.Desktop(1, GridCell(0, 0)), app("p1")),
                HomeItem.Application(3, Placement.Desktop(2, GridCell(1, 1)), app("p2")),
            ),
        )
        val removed = HomeLayoutEditor.removePage(layout, 1)
        assertEquals(2, removed.pageCount)
        assertTrue(removed.items.none { it is HomeItem.Application && it.app == app("p1") })
        assertEquals(Placement.Desktop(1, GridCell(1, 1)), removed.placementOf("p2"))
    }

    @Test
    fun `removeEmptyPages compacts page indices`() {
        val layout = HomeLayout(
            pageCount = 4,
            items = listOf(
                HomeItem.Application(1, Placement.Desktop(0, GridCell(0, 0)), app("a")),
                HomeItem.Application(2, Placement.Desktop(3, GridCell(0, 0)), app("b")),
            ),
        )
        val compacted = HomeLayoutEditor.removeEmptyPages(layout)
        assertEquals(2, compacted.pageCount)
        assertEquals(Placement.Desktop(1, GridCell(0, 0)), compacted.placementOf("b"))
    }

    @Test
    fun `reflow moves items that fall outside a smaller grid`() {
        val layout = HomeLayout(
            pageCount = 1,
            items = listOf(
                HomeItem.Application(1, Placement.Desktop(0, GridCell(4, 4)), app("far")),
                HomeItem.Application(2, Placement.Desktop(0, GridCell(0, 0)), app("near")),
            ),
        )
        val reflowed = HomeLayoutEditor.reflow(layout, grid, dockSlots = 4)
        val far = reflowed.placementOf("far") as Placement.Desktop
        assertTrue(grid.contains(far.cell))
        assertEquals(Placement.Desktop(0, GridCell(0, 0)), reflowed.placementOf("near"))
    }

    @Test
    fun `reflow moves dock overflow onto the desktop`() {
        val layout = HomeLayout.Empty.addDockApp("d0").addDockApp("d1").addDockApp("d2")
        val reflowed = HomeLayoutEditor.reflow(layout, grid, dockSlots = 2)
        assertEquals(2, reflowed.dockItems.size)
        assertTrue(reflowed.placementOf("d2") is Placement.Desktop)
    }

    @Test
    fun `reflow separates overlapping items`() {
        val layout = HomeLayout(
            pageCount = 1,
            items = listOf(
                HomeItem.Application(1, Placement.Desktop(0, GridCell(1, 1)), app("a")),
                HomeItem.Application(2, Placement.Desktop(0, GridCell(1, 1)), app("b")),
            ),
        )
        val reflowed = HomeLayoutEditor.reflow(layout, grid, dockSlots = 4)
        assertTrue(reflowed.placementOf("a") != reflowed.placementOf("b"))
    }

    @Test
    fun `removeMissing drops uninstalled apps and shortcuts`() {
        val layout = HomeLayout.Empty.addApp("keep").addApp("gone").let {
            HomeLayoutEditor.addToDesktop(it, grid, GridSpan.Single, 0) { id, placement ->
                HomeItem.Shortcut(id, placement, ShortcutKey("pkg.gone", "s1", 0L), "Shortcut")
            }
        }
        val cleaned = HomeLayoutEditor.removeMissing(
            layout,
            keepApp = { it == app("keep") },
            keepShortcut = { it.packageName != "pkg.gone" },
        )
        assertEquals(listOf(app("keep")), cleaned.items.map { (it as HomeItem.Application).app })
    }

    @Test
    fun `addToDock returns null when full`() {
        val layout = HomeLayout.Empty.addDockApp("d0", 1)
        assertNull(
            HomeLayoutEditor.addToDock(layout, 1, null) { id, placement ->
                HomeItem.Application(id, placement, app("x"))
            },
        )
    }

    @Test
    fun `addToDesktopAt prefers the requested cell`() {
        val layout = HomeLayoutEditor.addToDesktopAt(HomeLayout.Empty, grid, GridSpan.Single, 0, GridCell(3, 4)) { id, placement ->
            HomeItem.Application(id, placement, app("a"))
        }
        assertEquals(Placement.Desktop(0, GridCell(3, 4)), layout.placementOf("a"))
    }
}
