package com.abrarshakhi.bricks.core.domain.usecase.home

import com.abrarshakhi.bricks.core.domain.layout.HomeLayoutEditor
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.GridCell
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.HomeItem
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.model.HomeSettings
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.model.WidgetProvider
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import com.abrarshakhi.bricks.core.domain.repository.HomeLayoutRepository
import com.abrarshakhi.bricks.core.domain.repository.SettingsRepository
import com.abrarshakhi.bricks.core.domain.repository.ShortcutRepository
import com.abrarshakhi.bricks.core.domain.repository.WidgetProviderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

sealed interface DropTarget {
    data class Desktop(val page: Int, val cell: GridCell) : DropTarget
    data class Dock(val index: Int) : DropTarget
    data class Folder(val targetItemId: Long, val defaultTitle: String) : DropTarget
    data class OutOfFolder(val page: Int) : DropTarget
    data class FolderRank(val rank: Int) : DropTarget
}

class ObserveHomeLayoutUseCase(
    private val repository: HomeLayoutRepository,
) {
    operator fun invoke(): Flow<HomeLayout> = repository.layout
}

internal suspend fun SettingsRepository.homeSettings(): HomeSettings = settings.first().home

internal fun HomeSettings.gridOrDefault(): GridSize = grid ?: GridSize.of(4, 5)

class AddAppToHomeUseCase(
    private val repository: HomeLayoutRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(app: AppKey, preferredPage: Int) {
        val grid = settingsRepository.homeSettings().gridOrDefault()
        repository.update { layout ->
            HomeLayoutEditor.addToDesktop(layout, grid, GridSpan.Single, preferredPage) { id, placement ->
                HomeItem.Application(id = id, placement = placement, app = app)
            }
        }
    }
}

class AddAppToDockUseCase(
    private val repository: HomeLayoutRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(app: AppKey): Boolean {
        val slots = settingsRepository.homeSettings().dockSlots
        var added = false
        repository.update { layout ->
            HomeLayoutEditor.addToDock(layout, slots, null) { id, placement ->
                HomeItem.Application(id = id, placement = placement, app = app)
            }?.also { added = true } ?: layout
        }
        return added
    }
}

class PinShortcutToHomeUseCase(
    private val repository: HomeLayoutRepository,
    private val settingsRepository: SettingsRepository,
    private val shortcutRepository: ShortcutRepository,
) {
    suspend operator fun invoke(shortcut: ShortcutKey, label: String, preferredPage: Int, alreadyPinned: Boolean = false) {
        if (!alreadyPinned) shortcutRepository.pin(shortcut)
        val grid = settingsRepository.homeSettings().gridOrDefault()
        repository.update { layout ->
            HomeLayoutEditor.addToDesktop(layout, grid, GridSpan.Single, preferredPage) { id, placement ->
                HomeItem.Shortcut(id = id, placement = placement, shortcut = shortcut, label = label)
            }
        }
    }
}

class FindWidgetProviderUseCase(
    private val repository: WidgetProviderRepository,
) {
    suspend operator fun invoke(key: WidgetProviderKey): WidgetProvider? = repository.find(key)
}

class AddWidgetToHomeUseCase(
    private val repository: HomeLayoutRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(
        appWidgetId: Int,
        provider: WidgetProviderKey,
        span: GridSpan,
        preferredPage: Int,
        cell: GridCell? = null,
    ): Long {
        val grid = settingsRepository.homeSettings().gridOrDefault()
        var createdId = -1L
        val create = { id: Long, placement: Placement ->
            createdId = id
            HomeItem.Widget(id = id, placement = placement, appWidgetId = appWidgetId, provider = provider)
        }
        repository.update { layout ->
            if (cell != null) {
                HomeLayoutEditor.addToDesktopAt(layout, grid, span, preferredPage, cell, create)
            } else {
                HomeLayoutEditor.addToDesktop(layout, grid, span, preferredPage, create)
            }
        }
        return createdId
    }
}

class DropAppOnHomeUseCase(
    private val repository: HomeLayoutRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(app: AppKey, target: DropTarget) {
        val settings = settingsRepository.homeSettings()
        val grid = settings.gridOrDefault()
        repository.update { layout ->
            val create = { id: Long, placement: Placement -> HomeItem.Application(id = id, placement = placement, app = app) }
            when (target) {
                is DropTarget.Desktop ->
                    HomeLayoutEditor.addToDesktopAt(layout, grid, GridSpan.Single, target.page, target.cell, create)

                is DropTarget.Dock ->
                    HomeLayoutEditor.addToDock(layout, settings.dockSlots, target.index, create)
                        ?: HomeLayoutEditor.addToDesktop(layout, grid, GridSpan.Single, 0, create)

                is DropTarget.Folder -> {
                    val added = HomeLayoutEditor.addToDesktop(layout, grid, GridSpan.Single, 0, create)
                    HomeLayoutEditor.mergeIntoFolder(added, target.targetItemId, layout.nextId, target.defaultTitle)
                }

                is DropTarget.OutOfFolder, is DropTarget.FolderRank ->
                    HomeLayoutEditor.addToDesktop(layout, grid, GridSpan.Single, 0, create)
            }
        }
    }
}

class MoveHomeItemUseCase(
    private val repository: HomeLayoutRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(itemId: Long, target: DropTarget) {
        val settings = settingsRepository.homeSettings()
        val grid = settings.gridOrDefault()
        repository.update { layout ->
            when (target) {
                is DropTarget.Desktop -> HomeLayoutEditor.moveToDesktop(layout, grid, itemId, target.page, target.cell)
                is DropTarget.Dock -> HomeLayoutEditor.moveToDock(layout, itemId, target.index, settings.dockSlots)
                is DropTarget.Folder -> HomeLayoutEditor.mergeIntoFolder(layout, target.targetItemId, itemId, target.defaultTitle)
                is DropTarget.OutOfFolder -> HomeLayoutEditor.moveOutOfFolder(layout, grid, itemId, target.page)
                is DropTarget.FolderRank -> HomeLayoutEditor.moveWithinFolder(layout, itemId, target.rank)
            }
        }
    }
}

class RemoveHomeItemUseCase(
    private val repository: HomeLayoutRepository,
    private val shortcutRepository: ShortcutRepository,
) {
    suspend operator fun invoke(itemId: Long): Set<Int> {
        var released = emptySet<Int>()
        var unpinned = emptySet<ShortcutKey>()
        repository.update { layout ->
            val updated = HomeLayoutEditor.remove(layout, itemId)
            released = HomeLayoutEditor.widgetIds(layout) - HomeLayoutEditor.widgetIds(updated)
            unpinned = layout.shortcutKeys() - updated.shortcutKeys()
            updated
        }
        unpinned.forEach { shortcutRepository.unpin(it) }
        return released
    }

    private fun HomeLayout.shortcutKeys(): Set<ShortcutKey> =
        items.filterIsInstance<HomeItem.Shortcut>().map { it.shortcut }.toSet()
}

class RenameFolderUseCase(
    private val repository: HomeLayoutRepository,
) {
    suspend operator fun invoke(folderId: Long, title: String) {
        val cleaned = title.trim().take(MAX_TITLE_LENGTH)
        repository.update { layout -> HomeLayoutEditor.renameFolder(layout, folderId, cleaned) }
    }

    private companion object {
        const val MAX_TITLE_LENGTH = 32
    }
}

class ResizeWidgetUseCase(
    private val repository: HomeLayoutRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(itemId: Long, span: GridSpan): Boolean {
        val grid = settingsRepository.homeSettings().gridOrDefault()
        var changed = false
        repository.update { layout ->
            HomeLayoutEditor.resizeWidget(layout, grid, itemId, span).also { changed = it != layout }
        }
        return changed
    }
}

class AddHomePageUseCase(
    private val repository: HomeLayoutRepository,
) {
    suspend operator fun invoke(): Int {
        val updated = repository.update { layout -> HomeLayoutEditor.addPage(layout) }
        return updated.pageCount - 1
    }
}

class RemoveHomePageUseCase(
    private val repository: HomeLayoutRepository,
) {
    suspend operator fun invoke(page: Int): Set<Int> {
        var released = emptySet<Int>()
        repository.update { layout ->
            val updated = HomeLayoutEditor.removePage(layout, page)
            released = HomeLayoutEditor.widgetIds(layout) - HomeLayoutEditor.widgetIds(updated)
            updated
        }
        return released
    }
}

class CompactHomePagesUseCase(
    private val repository: HomeLayoutRepository,
) {
    suspend operator fun invoke() {
        repository.update { layout -> HomeLayoutEditor.removeEmptyPages(layout) }
    }
}
