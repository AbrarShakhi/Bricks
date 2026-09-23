package com.abrarshakhi.bricks.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.domain.model.IconStyle
import com.abrarshakhi.bricks.core.ui.component.AppIconBadges
import com.abrarshakhi.bricks.core.ui.component.AppIconShape
import com.abrarshakhi.bricks.core.ui.component.AppIconTile
import com.abrarshakhi.bricks.core.ui.component.AppLabel
import com.abrarshakhi.bricks.core.ui.component.InkTile
import com.abrarshakhi.bricks.core.ui.component.ItemActionMenu
import com.abrarshakhi.bricks.core.ui.component.ItemMenuEntry
import com.abrarshakhi.bricks.core.ui.dragdrop.DragDropController
import com.abrarshakhi.bricks.core.ui.dragdrop.DragPayload
import com.abrarshakhi.bricks.core.ui.dragdrop.LocalDragDropController
import com.abrarshakhi.bricks.core.ui.gesture.LauncherItemGestureCallbacks
import com.abrarshakhi.bricks.core.ui.gesture.launcherItemGestures
import com.abrarshakhi.bricks.core.ui.icon.rememberAppIcon
import com.abrarshakhi.bricks.core.ui.icon.rememberShortcutIcon
import com.abrarshakhi.bricks.core.ui.util.CoordinatesHolder
import com.abrarshakhi.bricks.core.ui.widget.AppWidget
import com.abrarshakhi.bricks.feature.home.HomeIntent
import com.abrarshakhi.bricks.feature.home.HomeTile
import com.abrarshakhi.bricks.feature.home.R
import com.abrarshakhi.bricks.feature.home.TileAction
import com.abrarshakhi.bricks.feature.home.TileMenu
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiSurface
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme
import com.abrarshakhi.bricks.core.ui.R as UiR

@Immutable
internal data class TileEnvironment(
    val iconSize: Dp,
    val showLabel: Boolean,
    val iconStyle: IconStyle,
    val onWallpaper: Boolean,
    val editMode: Boolean,
    val dragEnabled: Boolean,
    val page: Int,
    val reconfigurableWidgets: Set<Int> = emptySet(),
)

@Composable
internal fun HomeTileView(
    tile: HomeTile,
    environment: TileEnvironment,
    menu: TileMenu?,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    inFolder: Boolean = false,
    labelStyle: TextStyle = PuiTheme.typography.labelMedium,
) {
    if (tile is HomeTile.Widget) {
        WidgetTileView(tile, environment, menu, onIntent, modifier)
        return
    }
    val controller = LocalDragDropController.current
    val holder = remember { CoordinatesHolder() }
    var pressed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val isDragged = (controller.session?.payload as? DragPayload.HomeItem)?.itemId == tile.id
    val callbacks = object : LauncherItemGestureCallbacks {
        override fun onPressChanged(pressed: Boolean) {
            setPressed(pressed)
        }

        override fun onTap() {
            if (environment.editMode) {
                if (tile is HomeTile.Folder) onIntent(HomeIntent.OpenFolder(tile.id))
                return
            }
            when (tile) {
                is HomeTile.App -> onIntent(HomeIntent.LaunchApp(tile.app, holder.bounds))
                is HomeTile.Shortcut -> onIntent(HomeIntent.LaunchShortcut(tile.shortcut, holder.bounds))
                is HomeTile.Folder -> onIntent(HomeIntent.OpenFolder(tile.id))
                is HomeTile.Widget -> Unit
            }
        }

        override fun onLongPress() {
            onIntent(HomeIntent.ShowTileMenu(tile.id))
        }

        override fun onDragStart(rootPosition: Offset, grabOffset: Offset) {
            onIntent(HomeIntent.DismissMenus)
            if (inFolder) onIntent(HomeIntent.CloseFolder)
            controller.startTileDrag(
                tile = tile,
                inFolder = inFolder,
                rootPosition = rootPosition,
                grabOffset = grabOffset,
                ghostSize = holder.dpSize(density) ?: DpSize(environment.iconSize, environment.iconSize),
            ) {
                TileContent(tile = tile, environment = environment, pressed = false, onWallpaper = true, labelStyle = labelStyle)
            }
        }

        override fun onDrag(rootPosition: Offset) {
            controller.move(rootPosition)
        }

        override fun onDragEnd() {
            controller.drop()
        }

        override fun onDragCancel() {
            controller.cancel()
        }

        private fun setPressed(value: Boolean) {
            pressed = value
        }
    }
    Box(
        modifier = modifier
            .onGloballyPositioned { holder.coordinates = it }
            .alpha(if (isDragged) 0f else 1f)
            .launcherItemGestures(callbacks, dragEnabled = environment.dragEnabled),
        contentAlignment = Alignment.Center,
    ) {
        TileContent(
            tile = tile,
            environment = environment,
            pressed = pressed,
            onWallpaper = environment.onWallpaper && !inFolder,
            labelStyle = labelStyle,
        )
        TileMenuPopup(tile, environment, menu, onIntent, holder)
    }
}

@Composable
private fun WidgetTileView(
    tile: HomeTile.Widget,
    environment: TileEnvironment,
    menu: TileMenu?,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier,
) {
    val controller = LocalDragDropController.current
    val holder = remember { CoordinatesHolder() }
    val density = LocalDensity.current
    val isDragged = (controller.session?.payload as? DragPayload.HomeItem)?.itemId == tile.id
    Box(
        modifier = modifier
            .onGloballyPositioned { holder.coordinates = it }
            .alpha(if (isDragged) 0f else 1f),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(WIDGET_GUTTER)) {
            InkTile(
                modifier = Modifier.fillMaxSize(),
                shape = PuiTheme.shapes.large,
                color = PuiTheme.colors.surface,
                elevation = PuiTheme.elevation.level2,
            ) {
                AppWidget(
                    appWidgetId = tile.appWidgetId,
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    onLongPress = { onIntent(HomeIntent.ShowTileMenu(tile.id)) },
                )
            }
        }
        if (environment.editMode) {
            val callbacks = object : LauncherItemGestureCallbacks {
                override fun onTap() {
                    onIntent(HomeIntent.ShowTileMenu(tile.id))
                }

                override fun onLongPress() {
                    onIntent(HomeIntent.ShowTileMenu(tile.id))
                }

                override fun onDragStart(rootPosition: Offset, grabOffset: Offset) {
                    onIntent(HomeIntent.DismissMenus)
                    controller.startTileDrag(
                        tile = tile,
                        inFolder = false,
                        rootPosition = rootPosition,
                        grabOffset = grabOffset,
                        ghostSize = holder.dpSize(density) ?: DpSize(environment.iconSize, environment.iconSize),
                    ) { WidgetGhost() }
                }

                override fun onDrag(rootPosition: Offset) {
                    controller.move(rootPosition)
                }

                override fun onDragEnd() {
                    controller.drop()
                }

                override fun onDragCancel() {
                    controller.cancel()
                }
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .launcherItemGestures(callbacks),
                contentAlignment = Alignment.TopEnd,
            ) {
                PuiSurface(
                    modifier = Modifier.padding(2.dp),
                    shape = PuiTheme.shapes.pill,
                    color = PuiTheme.colors.accent,
                    elevation = PuiTheme.elevation.level1,
                ) {
                    PuiIcon(
                        BricksIcons.Resize,
                        contentDescription = stringResource(R.string.home_menu_resize),
                        size = 18.dp,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }
        }
        TileMenuPopup(tile, environment, menu, onIntent, holder)
    }
}

@Composable
private fun WidgetGhost() {
    InkTile(
        modifier = Modifier.fillMaxSize().padding(WIDGET_GUTTER),
        shape = PuiTheme.shapes.large,
        color = PuiTheme.colors.surfaceMuted,
        elevation = PuiTheme.elevation.level3,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PuiIcon(BricksIcons.Widgets, contentDescription = null)
            PuiText(stringResource(R.string.home_a11y_widget), style = PuiTheme.typography.labelMedium)
        }
    }
}

private fun CoordinatesHolder.dpSize(density: androidx.compose.ui.unit.Density): DpSize? {
    val size = coordinates?.takeIf { it.isAttached }?.size ?: return null
    return with(density) { DpSize(size.width.toDp(), size.height.toDp()) }
}

private fun DragDropController.startTileDrag(
    tile: HomeTile,
    inFolder: Boolean,
    rootPosition: Offset,
    grabOffset: Offset,
    ghostSize: DpSize,
    ghost: @Composable () -> Unit,
) {
    start(
        payload = DragPayload.HomeItem(tile.id, fromFolder = inFolder),
        rootPosition = rootPosition,
        grabOffset = grabOffset,
        ghostSize = ghostSize,
        ghost = ghost,
    )
}

@Composable
private fun TileMenuPopup(
    tile: HomeTile,
    environment: TileEnvironment,
    menu: TileMenu?,
    onIntent: (HomeIntent) -> Unit,
    holder: CoordinatesHolder,
) {
    if (menu?.tileId != tile.id) return
    ItemActionMenu(
        expanded = true,
        onDismiss = { onIntent(HomeIntent.DismissMenus) },
        entries = tileMenuEntries(tile, environment),
        onEntry = { action -> onIntent(HomeIntent.TileMenuAction(tile.id, action)) },
        title = tileTitle(tile).takeIf { it.isNotBlank() },
        shortcuts = menu.shortcuts,
        shortcutsLoading = menu.loadingShortcuts,
        shortcutOwner = (tile as? HomeTile.App)?.app,
        iconStyle = environment.iconStyle,
        onShortcut = { shortcut -> onIntent(HomeIntent.ShortcutFromMenu(shortcut, holder.bounds)) },
        onPinShortcut = { shortcut -> onIntent(HomeIntent.PinShortcutFromMenu(shortcut, environment.page)) },
    )
}

@Composable
private fun tileTitle(tile: HomeTile): String = when (tile) {
    is HomeTile.App -> tile.label
    is HomeTile.Shortcut -> tile.label
    is HomeTile.Folder -> tile.title.ifBlank { stringResource(R.string.home_folder_default) }
    is HomeTile.Widget -> ""
}

@Composable
private fun tileMenuEntries(tile: HomeTile, environment: TileEnvironment): List<ItemMenuEntry<TileAction>> {
    val appInfo = ItemMenuEntry(TileAction.AppInfo, stringResource(UiR.string.ui_menu_app_info), BricksIcons.Info)
    val remove = ItemMenuEntry(TileAction.Remove, stringResource(UiR.string.ui_menu_remove), BricksIcons.Close)
    return when (tile) {
        is HomeTile.App -> buildList {
            add(appInfo)
            add(ItemMenuEntry(TileAction.Rename, stringResource(UiR.string.ui_menu_rename), BricksIcons.Label))
            add(ItemMenuEntry(TileAction.Hide, stringResource(UiR.string.ui_menu_hide), BricksIcons.EyeOff))
            add(remove)
            if (!tile.isSystem) {
                add(
                    ItemMenuEntry(
                        TileAction.Uninstall,
                        stringResource(UiR.string.ui_menu_uninstall),
                        BricksIcons.Trash,
                        destructive = true,
                    ),
                )
            }
        }
        is HomeTile.Shortcut -> listOfNotNull(appInfo.takeIf { tile.owner != null }, remove)
        is HomeTile.Folder -> listOf(
            ItemMenuEntry(TileAction.RenameFolder, stringResource(UiR.string.ui_menu_rename), BricksIcons.Edit),
            remove,
        )
        is HomeTile.Widget -> buildList {
            add(ItemMenuEntry(TileAction.Resize, stringResource(R.string.home_menu_resize), BricksIcons.Resize))
            if (tile.appWidgetId in environment.reconfigurableWidgets) {
                add(ItemMenuEntry(TileAction.Reconfigure, stringResource(R.string.home_menu_reconfigure), BricksIcons.Settings))
            }
            add(remove)
        }
    }
}

@Composable
internal fun TileContent(
    tile: HomeTile,
    environment: TileEnvironment,
    pressed: Boolean,
    onWallpaper: Boolean,
    labelStyle: TextStyle,
) {
    when (tile) {
        is HomeTile.App -> IconWithLabel(
            label = tile.label,
            showLabel = environment.showLabel,
            onWallpaper = onWallpaper,
            labelStyle = labelStyle,
            description = tile.label,
        ) {
            AppIconBadges(badgeCount = tile.badge, isWork = tile.isWork) {
                AppIconTile(
                    icon = rememberAppIcon(tile.app, environment.iconSize, environment.iconStyle),
                    style = environment.iconStyle,
                    size = environment.iconSize,
                    pressed = pressed,
                    dimmed = tile.isPaused,
                )
            }
        }
        is HomeTile.Shortcut -> IconWithLabel(
            label = tile.label,
            showLabel = environment.showLabel,
            onWallpaper = onWallpaper,
            labelStyle = labelStyle,
            description = tile.label,
        ) {
            AppIconBadges(badgeCount = tile.badge) {
                AppIconTile(
                    icon = rememberShortcutIcon(tile.shortcut, tile.owner, environment.iconSize, environment.iconStyle),
                    style = environment.iconStyle,
                    size = environment.iconSize,
                    pressed = pressed,
                )
            }
        }
        is HomeTile.Folder -> {
            val title = tile.title.ifBlank { stringResource(R.string.home_folder_default) }
            val description = pluralStringResource(R.plurals.home_a11y_folder, tile.children.size, title, tile.children.size)
            IconWithLabel(
                label = title,
                showLabel = environment.showLabel,
                onWallpaper = onWallpaper,
                labelStyle = labelStyle,
                description = description,
            ) {
                AppIconBadges(badgeCount = tile.badge) {
                    FolderIcon(tile, environment.iconSize, environment.iconStyle, pressed)
                }
            }
        }
        is HomeTile.Widget -> WidgetGhost()
    }
}

@Composable
private fun IconWithLabel(
    label: String,
    showLabel: Boolean,
    onWallpaper: Boolean,
    labelStyle: TextStyle,
    description: String,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            AppLabel(
                text = label,
                onWallpaper = onWallpaper,
                style = labelStyle,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
internal fun FolderIcon(folder: HomeTile.Folder, size: Dp, style: IconStyle, pressed: Boolean) {
    val miniStyle = IconStyle.Classic
    val miniSize = size * 0.36f
    val gap = size * 0.06f
    InkTile(
        modifier = Modifier.size(size),
        pressed = pressed,
        shape = AppIconShape,
        color = PuiTheme.colors.surfaceMuted,
        elevation = if (size >= 44.dp) PuiTheme.elevation.level2 else PuiTheme.elevation.level1,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            folder.children.take(4).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { child ->
                        val icon = when (child) {
                            is HomeTile.App -> rememberAppIcon(child.app, miniSize, miniStyle)
                            is HomeTile.Shortcut -> rememberShortcutIcon(child.shortcut, child.owner, miniSize, miniStyle)
                            else -> null
                        }
                        AppIconTile(
                            icon = icon,
                            style = IconStyle.Classic,
                            size = miniSize,
                            grayscale = style == IconStyle.Ink,
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.size(miniSize))
                }
            }
        }
    }
}

private val WIDGET_GUTTER = 6.dp
