package com.abrarshakhi.bricks.core.data.home

import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.GridCell
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.HomeItem
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import kotlinx.serialization.Serializable

@Serializable
internal data class HomeLayoutDto(
    val version: Int = 1,
    val initialized: Boolean = false,
    val pageCount: Int = 1,
    val items: List<HomeItemDto> = emptyList(),
)

@Serializable
internal data class HomeItemDto(
    val id: Long,
    val type: String,
    val container: String,
    val page: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val index: Int = 0,
    val folderId: Long = 0,
    val packageName: String? = null,
    val className: String? = null,
    val userSerial: Long = 0,
    val title: String? = null,
    val appWidgetId: Int = 0,
    val shortcutId: String? = null,
    val label: String? = null,
)

private object ItemType {
    const val APP = "app"
    const val FOLDER = "folder"
    const val WIDGET = "widget"
    const val SHORTCUT = "shortcut"
}

private object Container {
    const val DESKTOP = "desktop"
    const val DOCK = "dock"
    const val FOLDER = "folder"
}

internal fun HomeLayoutDto.toDomain(): HomeLayout {
    val pages = pageCount.coerceAtLeast(1)
    return HomeLayout(
        pageCount = pages,
        items = items.mapNotNull { it.toDomain() },
        isInitialized = initialized,
    )
}

internal fun HomeLayout.toDto(): HomeLayoutDto = HomeLayoutDto(
    initialized = isInitialized,
    pageCount = pageCount,
    items = items.map { it.toDto() },
)

private fun HomeItemDto.placement(): Placement? = when (container) {
    Container.DESKTOP -> Placement.Desktop(
        page = page.coerceAtLeast(0),
        cell = GridCell(x, y),
        span = GridSpan(spanX.coerceAtLeast(1), spanY.coerceAtLeast(1)),
    )
    Container.DOCK -> Placement.Dock(index.coerceAtLeast(0))
    Container.FOLDER -> Placement.InFolder(folderId, index.coerceAtLeast(0))
    else -> null
}

private fun HomeItemDto.toDomain(): HomeItem? {
    val placement = placement() ?: return null
    return when (type) {
        ItemType.APP -> {
            val pkg = packageName ?: return null
            val cls = className ?: return null
            HomeItem.Application(id, placement, AppKey(pkg, cls, userSerial))
        }
        ItemType.FOLDER -> HomeItem.Folder(id, placement, title.orEmpty())
        ItemType.WIDGET -> {
            val pkg = packageName ?: return null
            val cls = className ?: return null
            HomeItem.Widget(id, placement, appWidgetId, WidgetProviderKey(pkg, cls, userSerial))
        }
        ItemType.SHORTCUT -> {
            val pkg = packageName ?: return null
            val shortcut = shortcutId ?: return null
            HomeItem.Shortcut(id, placement, ShortcutKey(pkg, shortcut, userSerial), label.orEmpty())
        }
        else -> null
    }
}

private fun HomeItem.toDto(): HomeItemDto {
    val base = when (val placement = placement) {
        is Placement.Desktop -> HomeItemDto(
            id = id,
            type = "",
            container = Container.DESKTOP,
            page = placement.page,
            x = placement.cell.x,
            y = placement.cell.y,
            spanX = placement.span.width,
            spanY = placement.span.height,
        )
        is Placement.Dock -> HomeItemDto(id = id, type = "", container = Container.DOCK, index = placement.index)
        is Placement.InFolder -> HomeItemDto(
            id = id,
            type = "",
            container = Container.FOLDER,
            folderId = placement.folderId,
            index = placement.rank,
        )
    }
    return when (this) {
        is HomeItem.Application -> base.copy(
            type = ItemType.APP,
            packageName = app.packageName,
            className = app.className,
            userSerial = app.userSerial,
        )
        is HomeItem.Folder -> base.copy(type = ItemType.FOLDER, title = title)
        is HomeItem.Widget -> base.copy(
            type = ItemType.WIDGET,
            packageName = provider.packageName,
            className = provider.className,
            userSerial = provider.userSerial,
            appWidgetId = appWidgetId,
        )
        is HomeItem.Shortcut -> base.copy(
            type = ItemType.SHORTCUT,
            packageName = shortcut.packageName,
            shortcutId = shortcut.shortcutId,
            userSerial = shortcut.userSerial,
            label = label,
        )
    }
}
