package com.abrarshakhi.bricks.core.domain.model

sealed interface Placement {
    data class Desktop(
        val page: Int,
        val cell: GridCell,
        val span: GridSpan = GridSpan.Single,
    ) : Placement

    data class Dock(val index: Int) : Placement

    data class InFolder(
        val folderId: Long,
        val rank: Int,
    ) : Placement
}

data class WidgetProviderKey(
    val packageName: String,
    val className: String,
    val userSerial: Long,
)

data class ShortcutKey(
    val packageName: String,
    val shortcutId: String,
    val userSerial: Long,
)

sealed interface HomeItem {
    val id: Long
    val placement: Placement

    fun withPlacement(placement: Placement): HomeItem

    data class Application(
        override val id: Long,
        override val placement: Placement,
        val app: AppKey,
    ) : HomeItem {
        override fun withPlacement(placement: Placement): HomeItem = copy(placement = placement)
    }

    data class Folder(
        override val id: Long,
        override val placement: Placement,
        val title: String,
    ) : HomeItem {
        override fun withPlacement(placement: Placement): HomeItem = copy(placement = placement)
    }

    data class Widget(
        override val id: Long,
        override val placement: Placement,
        val appWidgetId: Int,
        val provider: WidgetProviderKey,
    ) : HomeItem {
        override fun withPlacement(placement: Placement): HomeItem = copy(placement = placement)
    }

    data class Shortcut(
        override val id: Long,
        override val placement: Placement,
        val shortcut: ShortcutKey,
        val label: String,
    ) : HomeItem {
        override fun withPlacement(placement: Placement): HomeItem = copy(placement = placement)
    }
}

val HomeItem.canLiveInFolder: Boolean
    get() = this is HomeItem.Application || this is HomeItem.Shortcut

val HomeItem.canLiveInDock: Boolean
    get() = this !is HomeItem.Widget

data class HomeLayout(
    val pageCount: Int,
    val items: List<HomeItem>,
    val isInitialized: Boolean = true,
) {
    val dockItems: List<HomeItem>
        get() = items
            .filter { it.placement is Placement.Dock }
            .sortedBy { (it.placement as Placement.Dock).index }

    val nextId: Long
        get() = (items.maxOfOrNull { it.id } ?: 0L) + 1L

    fun desktopItems(page: Int): List<HomeItem> =
        items.filter { (it.placement as? Placement.Desktop)?.page == page }

    fun folderContents(folderId: Long): List<HomeItem> = items
        .filter { (it.placement as? Placement.InFolder)?.folderId == folderId }
        .sortedBy { (it.placement as Placement.InFolder).rank }

    fun find(id: Long): HomeItem? = items.firstOrNull { it.id == id }

    companion object {
        val Empty = HomeLayout(pageCount = 1, items = emptyList())
        val Uninitialized = HomeLayout(pageCount = 1, items = emptyList(), isInitialized = false)
    }
}
