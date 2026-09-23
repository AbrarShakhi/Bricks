package com.abrarshakhi.bricks.feature.home

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.GestureAction
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.HomeItem
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.model.LauncherSettings
import com.abrarshakhi.bricks.core.domain.model.PackageUserKey
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.model.ProfileType
import com.abrarshakhi.bricks.core.domain.usecase.system.GestureOutcome
import com.abrarshakhi.bricks.core.ui.mvi.MviViewModel
import com.abrarshakhi.bricks.core.ui.util.HomeGridMetrics
import com.abrarshakhi.bricks.core.ui.widget.LauncherWidgetHost
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeViewModel(
    private val observers: HomeObservers,
    private val appActions: HomeAppActions,
    private val layoutActions: HomeLayoutActions,
    private val system: HomeSystemActions,
    private val widgetHost: LauncherWidgetHost,
    private val gridMetrics: HomeGridMetrics,
) : MviViewModel<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    private var menuJob: Job? = null
    private var orphanWidgetsReleased = false
    private var appsByKey: Map<AppKey, App> = emptyMap()
    private var cellSizeDp: Pair<Float, Float> = DEFAULT_CELL_DP to DEFAULT_CELL_DP

    init {
        viewModelScope.launch {
            combine(
                observers.layout(),
                observers.apps(),
                observers.settings(),
                observers.badges(),
            ) { layout, apps, settings, badges -> Snapshot(layout, apps, settings, badges) }
                .collect(::applySnapshot)
        }
        viewModelScope.launch {
            observers.glance().collect { glance ->
                reduce {
                    copy(
                        glance = GlanceUi(
                            timeMillis = glance.timeMillis,
                            batteryLevel = glance.battery?.level,
                            charging = glance.battery?.isCharging == true,
                            nextAlarmMillis = glance.nextAlarmMillis,
                        ),
                    )
                }
            }
        }
        refreshSystemStatus()
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.DeviceMetrics -> viewModelScope.launch {
                layoutActions.ensureGrid(intent.smallestWidthDp, intent.largestWidthDp)
            }
            HomeIntent.Resumed -> refreshSystemStatus()
            is HomeIntent.LaunchApp -> viewModelScope.launch {
                if (!appActions.launchApp(intent.app, intent.bounds)) emit(HomeEffect.ShowMessage(HomeMessage.LaunchFailed))
            }
            is HomeIntent.LaunchShortcut -> {
                if (!appActions.launchShortcut(intent.shortcut, intent.bounds)) {
                    emit(HomeEffect.ShowMessage(HomeMessage.LaunchFailed))
                }
            }
            is HomeIntent.OpenFolder -> reduce { copy(openFolderId = intent.folderId, menu = null, homeMenuVisible = false) }
            HomeIntent.CloseFolder -> reduce { copy(openFolderId = null, menu = null) }
            is HomeIntent.ShowTileMenu -> showTileMenu(intent.tileId)
            HomeIntent.DismissMenus -> dismissMenus()
            is HomeIntent.TileMenuAction -> onTileAction(intent.tileId, intent.action)
            is HomeIntent.ShortcutFromMenu -> {
                dismissMenus()
                onIntent(HomeIntent.LaunchShortcut(intent.shortcut.key, intent.bounds))
            }
            is HomeIntent.PinShortcutFromMenu -> {
                dismissMenus()
                viewModelScope.launch {
                    layoutActions.pinShortcut(
                        shortcut = intent.shortcut.key,
                        label = intent.shortcut.shortLabel,
                        preferredPage = intent.page,
                        alreadyPinned = intent.shortcut.isPinned,
                    )
                }
            }
            is HomeIntent.MoveTile -> viewModelScope.launch { layoutActions.move(intent.tileId, intent.target) }
            is HomeIntent.DropApp -> viewModelScope.launch { layoutActions.dropApp(intent.app, intent.target) }
            is HomeIntent.RemoveTile -> viewModelScope.launch { removeTile(intent.tileId) }
            is HomeIntent.UninstallTile -> uninstallTile(intent.tileId)
            is HomeIntent.UninstallApp -> if (appsByKey[intent.app]?.isSystem == true) {
                emit(HomeEffect.ShowMessage(HomeMessage.CannotUninstall))
            } else {
                appActions.uninstall(intent.app)
            }
            is HomeIntent.RenameFolder -> {
                reduce { copy(renameFolderId = null) }
                viewModelScope.launch { layoutActions.renameFolder(intent.folderId, intent.title) }
            }
            is HomeIntent.RenameApp -> {
                val target = currentState.renameApp
                reduce { copy(renameApp = null) }
                if (target != null) {
                    viewModelScope.launch { appActions.rename(intent.app, intent.label, target.systemLabel) }
                }
            }
            HomeIntent.DismissRename -> reduce { copy(renameFolderId = null, renameApp = null) }
            is HomeIntent.ResizeWidget -> viewModelScope.launch {
                if (layoutActions.resizeWidget(intent.tileId, intent.span)) {
                    reduce { copy(resize = resize?.copy(span = intent.span)) }
                } else {
                    emit(HomeEffect.ShowMessage(HomeMessage.NoRoomForWidget))
                }
            }
            HomeIntent.DismissResize -> reduce { copy(resize = null) }
            HomeIntent.ShowHomeMenu -> reduce { copy(homeMenuVisible = true, menu = null) }
            is HomeIntent.HomeMenu -> onHomeMenu(intent.action)
            HomeIntent.ExitEditMode -> {
                reduce { copy(editMode = false) }
                viewModelScope.launch { layoutActions.compactPages() }
            }
            HomeIntent.AddPage -> viewModelScope.launch { emit(HomeEffect.ScrollToPage(layoutActions.addPage())) }
            is HomeIntent.RequestRemovePage -> {
                val page = currentState.pages.getOrNull(intent.page) ?: return
                if (page.tiles.isEmpty()) {
                    viewModelScope.launch { layoutActions.removePage(intent.page) }
                } else {
                    reduce { copy(confirmRemovePage = intent.page) }
                }
            }
            HomeIntent.ConfirmRemovePage -> {
                val page = currentState.confirmRemovePage ?: return
                reduce { copy(confirmRemovePage = null) }
                viewModelScope.launch { layoutActions.removePage(page).forEach(widgetHost::deleteId) }
            }
            HomeIntent.DismissRemovePage -> reduce { copy(confirmRemovePage = null) }
            is HomeIntent.Gesture -> onGesture(intent.gesture)
            is HomeIntent.HomePressed -> {
                menuJob?.cancel()
                reduce {
                    copy(
                        openFolderId = null,
                        menu = null,
                        homeMenuVisible = false,
                        editMode = false,
                        renameFolderId = null,
                        renameApp = null,
                        resize = null,
                        confirmRemovePage = null,
                    )
                }
                emit(HomeEffect.ResetHome(scrollToFirstPage = intent.alreadyVisible))
            }
            HomeIntent.OpenClock -> system.status.openClock()
            HomeIntent.OpenCalendar -> system.status.openCalendar(currentState.glance?.timeMillis ?: System.currentTimeMillis())
            HomeIntent.OpenBattery -> system.status.openBatteryUsage()
            HomeIntent.SetAsDefaultLauncher -> system.status.openDefaultLauncherSettings()
            HomeIntent.DismissDefaultPrompt -> reduce { copy(defaultPromptDismissed = true) }
            is HomeIntent.CellSizeChanged -> if (intent.widthDp > 0f && intent.heightDp > 0f) {
                cellSizeDp = intent.widthDp to intent.heightDp
                gridMetrics.update(currentState.grid, intent.landscape, intent.widthDp, intent.heightDp)
            }
            HomeIntent.OpenLockScreenSetup -> system.status.openLockScreenSetup()
        }
    }

    private fun applySnapshot(snapshot: Snapshot) {
        appsByKey = snapshot.apps.associateBy { it.key }
        val appsByPackage = snapshot.apps.groupBy { it.key.toPackageUserKey() }
        val layout = snapshot.layout
        fun resolve(item: HomeItem): HomeTile = when (item) {
            is HomeItem.Application -> {
                val app = appsByKey[item.app]
                HomeTile.App(
                    id = item.id,
                    placement = item.placement,
                    app = item.app,
                    label = app?.label ?: fallbackLabel(item.app.packageName),
                    isWork = app?.profile == ProfileType.Work,
                    isPaused = app?.isSuspended == true,
                    isSystem = app?.isSystem == true,
                    badge = snapshot.badges[item.app.toPackageUserKey()] ?: 0,
                )
            }
            is HomeItem.Shortcut -> HomeTile.Shortcut(
                id = item.id,
                placement = item.placement,
                shortcut = item.shortcut,
                label = item.label.ifBlank { fallbackLabel(item.shortcut.packageName) },
                owner = appsByPackage[PackageUserKey(item.shortcut.packageName, item.shortcut.userSerial)]
                    ?.firstOrNull()?.key,
                badge = 0,
            )
            is HomeItem.Folder -> HomeTile.Folder(
                id = item.id,
                placement = item.placement,
                title = item.title,
                children = layout.folderContents(item.id).map(::resolve),
            )
            is HomeItem.Widget -> HomeTile.Widget(
                id = item.id,
                placement = item.placement,
                appWidgetId = item.appWidgetId,
                provider = item.provider,
            )
        }
        val pages = (0 until layout.pageCount).map { page -> HomePage(page, layout.desktopItems(page).map(::resolve)) }
        val dock = layout.dockItems.map(::resolve)
        val settings = snapshot.settings
        reduce {
            val next = copy(
                ready = layout.isInitialized,
                appearance = settings.appearance,
                home = settings.home,
                gestures = settings.gestures,
                grid = settings.home.grid ?: GridSize.of(4, 5),
                pages = pages,
                dock = dock,
            )
            next.copy(
                menu = next.menu?.takeIf { next.findTile(it.tileId) != null },
                openFolderId = next.openFolderId?.takeIf { next.findTile(it) is HomeTile.Folder },
                renameFolderId = next.renameFolderId?.takeIf { next.findTile(it) is HomeTile.Folder },
                resize = next.resize?.takeIf { next.findTile(it.tileId) is HomeTile.Widget },
            )
        }
        if (!orphanWidgetsReleased && layout.isInitialized) {
            orphanWidgetsReleased = true
            releaseOrphanWidgets(layout)
        }
    }

    private fun releaseOrphanWidgets(layout: HomeLayout) {
        val used = layout.items.filterIsInstance<HomeItem.Widget>().map { it.appWidgetId }.toSet()
        (widgetHost.allocatedIds() - used).forEach(widgetHost::deleteId)
    }

    private fun showTileMenu(tileId: Long) {
        menuJob?.cancel()
        val tile = currentState.findTile(tileId) ?: return
        reduce { copy(menu = TileMenu(tileId, loadingShortcuts = tile is HomeTile.App), homeMenuVisible = false) }
        if (tile is HomeTile.App) {
            menuJob = viewModelScope.launch {
                val shortcuts = runCatching { appActions.shortcuts(tile.app) }.getOrDefault(emptyList())
                reduce {
                    val open = menu
                    if (open?.tileId == tileId) copy(menu = open.copy(shortcuts = shortcuts, loadingShortcuts = false)) else this
                }
            }
        }
    }

    private fun dismissMenus() {
        menuJob?.cancel()
        reduce { copy(menu = null, homeMenuVisible = false) }
    }

    private fun onTileAction(tileId: Long, action: TileAction) {
        val tile = currentState.findTile(tileId)
        dismissMenus()
        if (tile == null) return
        when (action) {
            TileAction.AppInfo -> when (tile) {
                is HomeTile.App -> appActions.appInfo(tile.app, null)
                is HomeTile.Shortcut -> tile.owner?.let { appActions.appInfo(it, null) }
                else -> Unit
            }
            TileAction.Remove -> viewModelScope.launch { removeTile(tileId) }
            TileAction.Rename -> if (tile is HomeTile.App) {
                val app = appsByKey[tile.app]
                reduce {
                    copy(
                        renameApp = RenameAppUi(
                            app = tile.app,
                            label = tile.label,
                            systemLabel = app?.systemLabel ?: tile.label,
                        ),
                    )
                }
            }
            TileAction.RenameFolder -> if (tile is HomeTile.Folder) reduce { copy(renameFolderId = tileId) }
            TileAction.Hide -> if (tile is HomeTile.App) {
                viewModelScope.launch {
                    appActions.setHidden(tile.app, true)
                    emit(HomeEffect.ShowMessage(HomeMessage.AppHidden))
                }
            }
            TileAction.Uninstall -> uninstallTile(tileId)
            TileAction.Resize -> if (tile is HomeTile.Widget) startResize(tile)
            TileAction.Reconfigure -> if (tile is HomeTile.Widget) emit(HomeEffect.ReconfigureWidget(tile.appWidgetId))
        }
    }

    private fun startResize(tile: HomeTile.Widget) {
        val placement = tile.placement as? Placement.Desktop ?: return
        viewModelScope.launch {
            val grid = currentState.grid
            val provider = layoutActions.findWidgetProvider(tile.provider)
            val (cellWidth, cellHeight) = cellSizeDp
            val minSpan = provider?.minimumSpan(cellWidth, cellHeight, grid) ?: GridSpan.Single
            val maxSpan = provider?.maximumSpan(cellWidth, cellHeight, grid) ?: GridSpan(grid.columns, grid.rows)
            reduce {
                copy(
                    resize = WidgetResizeUi(
                        tileId = tile.id,
                        span = placement.span,
                        minSpan = GridSpan(minOf(minSpan.width, placement.span.width), minOf(minSpan.height, placement.span.height)),
                        maxSpan = GridSpan(maxOf(maxSpan.width, placement.span.width), maxOf(maxSpan.height, placement.span.height)),
                    ),
                )
            }
        }
    }

    private suspend fun removeTile(tileId: Long) {
        layoutActions.remove(tileId).forEach(widgetHost::deleteId)
    }

    private fun uninstallTile(tileId: Long) {
        val tile = currentState.findTile(tileId) as? HomeTile.App ?: return
        if (tile.isSystem) {
            emit(HomeEffect.ShowMessage(HomeMessage.CannotUninstall))
        } else {
            appActions.uninstall(tile.app)
        }
    }

    private fun onHomeMenu(action: HomeMenuAction) {
        reduce { copy(homeMenuVisible = false) }
        when (action) {
            HomeMenuAction.EditHome -> reduce { copy(editMode = true, menu = null, openFolderId = null) }
            HomeMenuAction.Widgets -> emit(HomeEffect.OpenWidgetPicker)
            HomeMenuAction.Wallpaper -> system.status.openWallpaperPicker()
            HomeMenuAction.Settings -> emit(HomeEffect.OpenSettings)
        }
    }

    private fun onGesture(gesture: HomeGesture) {
        val action = when (gesture) {
            HomeGesture.SwipeDown -> currentState.gestures.swipeDown
            HomeGesture.DoubleTap -> currentState.gestures.doubleTap
        }
        if (action == GestureAction.None) return
        when (system.gesture(action)) {
            GestureOutcome.Handled, GestureOutcome.Ignored -> Unit
            GestureOutcome.OpenDrawer -> emit(HomeEffect.OpenDrawer(focusSearch = false))
            GestureOutcome.OpenSearch -> emit(HomeEffect.OpenDrawer(focusSearch = true))
            GestureOutcome.OpenSettings -> emit(HomeEffect.OpenSettings)
            GestureOutcome.LockScreenNeedsSetup -> emit(HomeEffect.ShowMessage(HomeMessage.LockScreenSetup))
            GestureOutcome.Failed -> emit(HomeEffect.ShowMessage(HomeMessage.GestureUnavailable))
        }
    }

    private fun refreshSystemStatus() {
        reduce { copy(isDefaultLauncher = system.status.isDefaultLauncher) }
    }

    private fun fallbackLabel(packageName: String): String =
        packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }

    private data class Snapshot(
        val layout: HomeLayout,
        val apps: List<App>,
        val settings: LauncherSettings,
        val badges: Map<PackageUserKey, Int>,
    )

    private companion object {
        const val DEFAULT_CELL_DP = 80f
    }
}
