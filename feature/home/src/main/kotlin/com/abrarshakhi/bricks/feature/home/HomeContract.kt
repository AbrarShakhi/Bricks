package com.abrarshakhi.bricks.feature.home

import androidx.compose.runtime.Immutable
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppShortcut
import com.abrarshakhi.bricks.core.domain.model.AppearanceSettings
import com.abrarshakhi.bricks.core.domain.model.GestureSettings
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.HomeSettings
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import com.abrarshakhi.bricks.core.domain.usecase.home.DropTarget

@Immutable
sealed interface HomeTile {
    val id: Long
    val placement: Placement

    @Immutable
    data class App(
        override val id: Long,
        override val placement: Placement,
        val app: AppKey,
        val label: String,
        val isWork: Boolean,
        val isPaused: Boolean,
        val isSystem: Boolean,
        val badge: Int,
    ) : HomeTile

    @Immutable
    data class Shortcut(
        override val id: Long,
        override val placement: Placement,
        val shortcut: ShortcutKey,
        val label: String,
        val owner: AppKey?,
        val badge: Int,
    ) : HomeTile

    @Immutable
    data class Folder(
        override val id: Long,
        override val placement: Placement,
        val title: String,
        val children: List<HomeTile>,
    ) : HomeTile {
        val badge: Int
            get() = children.sumOf {
                when (it) {
                    is App -> it.badge
                    is Shortcut -> it.badge
                    else -> 0
                }
            }
    }

    @Immutable
    data class Widget(
        override val id: Long,
        override val placement: Placement,
        val appWidgetId: Int,
        val provider: WidgetProviderKey,
    ) : HomeTile
}

val HomeTile.label: String
    get() = when (this) {
        is HomeTile.App -> label
        is HomeTile.Shortcut -> label
        is HomeTile.Folder -> title
        is HomeTile.Widget -> ""
    }

@Immutable
data class HomePage(
    val index: Int,
    val tiles: List<HomeTile>,
)

@Immutable
data class GlanceUi(
    val timeMillis: Long,
    val batteryLevel: Int?,
    val charging: Boolean,
    val nextAlarmMillis: Long?,
)

@Immutable
data class TileMenu(
    val tileId: Long,
    val shortcuts: List<AppShortcut> = emptyList(),
    val loadingShortcuts: Boolean = false,
)

@Immutable
data class WidgetResizeUi(
    val tileId: Long,
    val span: GridSpan,
    val minSpan: GridSpan,
    val maxSpan: GridSpan,
)

@Immutable
data class RenameAppUi(
    val app: AppKey,
    val label: String,
    val systemLabel: String,
)

@Immutable
data class HomeState(
    val ready: Boolean = false,
    val appearance: AppearanceSettings = AppearanceSettings(),
    val home: HomeSettings = HomeSettings(),
    val gestures: GestureSettings = GestureSettings(),
    val grid: GridSize = GridSize(4, 5),
    val pages: List<HomePage> = listOf(HomePage(0, emptyList())),
    val dock: List<HomeTile> = emptyList(),
    val glance: GlanceUi? = null,
    val openFolderId: Long? = null,
    val menu: TileMenu? = null,
    val homeMenuVisible: Boolean = false,
    val editMode: Boolean = false,
    val renameFolderId: Long? = null,
    val renameApp: RenameAppUi? = null,
    val resize: WidgetResizeUi? = null,
    val isDefaultLauncher: Boolean = true,
    val defaultPromptDismissed: Boolean = false,
    val confirmRemovePage: Int? = null,
) {
    val pageCount: Int
        get() = pages.size

    fun findTile(id: Long): HomeTile? {
        pages.forEach { page ->
            page.tiles.forEach { tile ->
                if (tile.id == id) return tile
                if (tile is HomeTile.Folder) tile.children.firstOrNull { it.id == id }?.let { return it }
            }
        }
        dock.forEach { tile ->
            if (tile.id == id) return tile
            if (tile is HomeTile.Folder) tile.children.firstOrNull { it.id == id }?.let { return it }
        }
        return null
    }

    val openFolder: HomeTile.Folder?
        get() = openFolderId?.let { findTile(it) as? HomeTile.Folder }

    val showDefaultPrompt: Boolean
        get() = ready && !isDefaultLauncher && !defaultPromptDismissed
}

enum class TileAction { AppInfo, Remove, Rename, Hide, Uninstall, Resize, Reconfigure, RenameFolder }

enum class HomeMenuAction { EditHome, Widgets, Wallpaper, Settings }

enum class HomeGesture { SwipeDown, DoubleTap }

sealed interface HomeIntent {
    data class DeviceMetrics(val smallestWidthDp: Int, val largestWidthDp: Int) : HomeIntent
    data object Resumed : HomeIntent
    data class LaunchApp(val app: AppKey, val bounds: ScreenBounds?) : HomeIntent
    data class LaunchShortcut(val shortcut: ShortcutKey, val bounds: ScreenBounds?) : HomeIntent
    data class OpenFolder(val folderId: Long) : HomeIntent
    data object CloseFolder : HomeIntent
    data class ShowTileMenu(val tileId: Long) : HomeIntent
    data object DismissMenus : HomeIntent
    data class TileMenuAction(val tileId: Long, val action: TileAction) : HomeIntent
    data class ShortcutFromMenu(val shortcut: AppShortcut, val bounds: ScreenBounds?) : HomeIntent
    data class PinShortcutFromMenu(val shortcut: AppShortcut, val page: Int) : HomeIntent
    data class MoveTile(val tileId: Long, val target: DropTarget) : HomeIntent
    data class DropApp(val app: AppKey, val target: DropTarget) : HomeIntent
    data class RemoveTile(val tileId: Long) : HomeIntent
    data class UninstallTile(val tileId: Long) : HomeIntent
    data class UninstallApp(val app: AppKey) : HomeIntent
    data class RenameFolder(val folderId: Long, val title: String) : HomeIntent
    data class RenameApp(val app: AppKey, val label: String?) : HomeIntent
    data object DismissRename : HomeIntent
    data class ResizeWidget(val tileId: Long, val span: GridSpan) : HomeIntent
    data object DismissResize : HomeIntent
    data object ShowHomeMenu : HomeIntent
    data class HomeMenu(val action: HomeMenuAction) : HomeIntent
    data object ExitEditMode : HomeIntent
    data object AddPage : HomeIntent
    data class RequestRemovePage(val page: Int) : HomeIntent
    data object ConfirmRemovePage : HomeIntent
    data object DismissRemovePage : HomeIntent
    data class Gesture(val gesture: HomeGesture) : HomeIntent
    data class HomePressed(val alreadyVisible: Boolean) : HomeIntent
    data object OpenClock : HomeIntent
    data object OpenCalendar : HomeIntent
    data object OpenBattery : HomeIntent
    data object SetAsDefaultLauncher : HomeIntent
    data object DismissDefaultPrompt : HomeIntent
    data class CellSizeChanged(val widthDp: Float, val heightDp: Float, val landscape: Boolean) : HomeIntent
    data object OpenLockScreenSetup : HomeIntent
}

enum class HomeMessage {
    LaunchFailed,
    DockFull,
    NoRoomForWidget,
    LockScreenSetup,
    GestureUnavailable,
    AppHidden,
    CannotUninstall,
}

sealed interface HomeEffect {
    data class OpenDrawer(val focusSearch: Boolean) : HomeEffect
    data object OpenSettings : HomeEffect
    data object OpenWidgetPicker : HomeEffect
    data class ReconfigureWidget(val appWidgetId: Int) : HomeEffect
    data class ResetHome(val scrollToFirstPage: Boolean) : HomeEffect
    data class ScrollToPage(val page: Int) : HomeEffect
    data class ShowMessage(val message: HomeMessage) : HomeEffect
}
