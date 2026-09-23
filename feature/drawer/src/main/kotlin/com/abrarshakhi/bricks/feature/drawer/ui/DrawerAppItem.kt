package com.abrarshakhi.bricks.feature.drawer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.domain.model.IconStyle
import com.abrarshakhi.bricks.core.domain.model.ProfileType
import com.abrarshakhi.bricks.core.ui.component.AppIconBadges
import com.abrarshakhi.bricks.core.ui.component.AppIconTile
import com.abrarshakhi.bricks.core.ui.component.AppLabel
import com.abrarshakhi.bricks.core.ui.component.ItemActionMenu
import com.abrarshakhi.bricks.core.ui.component.ItemMenuEntry
import com.abrarshakhi.bricks.core.ui.dragdrop.DragPayload
import com.abrarshakhi.bricks.core.ui.dragdrop.LocalDragDropController
import com.abrarshakhi.bricks.core.ui.drawer.LauncherDrawerState
import com.abrarshakhi.bricks.core.ui.gesture.LauncherItemGestureCallbacks
import com.abrarshakhi.bricks.core.ui.gesture.launcherItemGestures
import com.abrarshakhi.bricks.core.ui.icon.rememberAppIcon
import com.abrarshakhi.bricks.core.ui.util.CoordinatesHolder
import com.abrarshakhi.bricks.feature.drawer.DrawerApp
import com.abrarshakhi.bricks.feature.drawer.DrawerAppAction
import com.abrarshakhi.bricks.feature.drawer.DrawerIntent
import com.abrarshakhi.bricks.feature.drawer.DrawerMenu
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme
import kotlinx.coroutines.launch
import com.abrarshakhi.bricks.core.ui.R as UiR

@Immutable
internal data class DrawerItemStyle(
    val iconSize: Dp,
    val iconStyle: IconStyle,
    val showLabels: Boolean,
    val list: Boolean,
)

@Composable
internal fun DrawerAppItem(
    app: DrawerApp,
    style: DrawerItemStyle,
    menu: DrawerMenu?,
    drawerState: LauncherDrawerState,
    onIntent: (DrawerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = LocalDragDropController.current
    val holder = remember { CoordinatesHolder() }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val isDragged = (controller.session?.payload as? DragPayload.DrawerApp)?.app == app.key
    val callbacks = object : LauncherItemGestureCallbacks {
        override fun onPressChanged(pressed: Boolean) {
            setPressed(pressed)
        }

        override fun onTap() {
            onIntent(DrawerIntent.Launch(app.key, holder.bounds))
        }

        override fun onLongPress() {
            onIntent(DrawerIntent.ShowMenu(app.key))
        }

        override fun onDragStart(rootPosition: Offset, grabOffset: Offset) {
            onIntent(DrawerIntent.DismissMenu)
            val size = holder.coordinates?.takeIf { it.isAttached }?.size
            val ghostSize = if (size != null) {
                with(density) { DpSize(size.width.toDp(), size.height.toDp()) }
            } else {
                DpSize(style.iconSize, style.iconSize)
            }
            controller.start(
                payload = DragPayload.DrawerApp(app.key),
                rootPosition = rootPosition,
                grabOffset = grabOffset,
                ghostSize = ghostSize,
                ghost = { ItemContent(app, style.copy(list = style.list), pressed = false, onWallpaper = true) },
            )
            scope.launch { drawerState.close() }
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
            .launcherItemGestures(callbacks),
        contentAlignment = Alignment.Center,
    ) {
        ItemContent(app, style, pressed, onWallpaper = false)
        if (menu?.app == app.key) {
            ItemActionMenu(
                expanded = true,
                onDismiss = { onIntent(DrawerIntent.DismissMenu) },
                entries = menuEntries(app),
                onEntry = { action -> onIntent(DrawerIntent.MenuAction(app.key, action)) },
                title = app.label,
                shortcuts = menu.shortcuts,
                shortcutsLoading = menu.loadingShortcuts,
                shortcutOwner = app.key,
                iconStyle = style.iconStyle,
                onShortcut = { shortcut -> onIntent(DrawerIntent.LaunchShortcut(shortcut, holder.bounds)) },
                onPinShortcut = { shortcut -> onIntent(DrawerIntent.PinShortcut(shortcut)) },
            )
        }
    }
}

@Composable
private fun ItemContent(app: DrawerApp, style: DrawerItemStyle, pressed: Boolean, onWallpaper: Boolean) {
    val icon = rememberAppIcon(app.key, style.iconSize, style.iconStyle)
    val tile: @Composable () -> Unit = {
        AppIconBadges(badgeCount = app.badge, isWork = app.profile == ProfileType.Work) {
            AppIconTile(
                icon = icon,
                style = style.iconStyle,
                size = style.iconSize,
                pressed = pressed,
                dimmed = app.isPaused,
            )
        }
    }
    if (style.list) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = style.iconSize + 20.dp)
                .padding(horizontal = PuiTheme.spacing.sm)
                .semantics(mergeDescendants = true) { contentDescription = app.label },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tile()
            Spacer(Modifier.width(PuiTheme.spacing.lg))
            PuiText(
                text = app.label,
                style = PuiTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PuiTheme.spacing.sm)
                .semantics(mergeDescendants = true) { contentDescription = app.label },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            tile()
            if (style.showLabels) {
                Spacer(Modifier.height(PuiTheme.spacing.sm))
                AppLabel(
                    text = app.label,
                    onWallpaper = onWallpaper,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = PuiTheme.spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun menuEntries(app: DrawerApp): List<ItemMenuEntry<DrawerAppAction>> = buildList {
    add(ItemMenuEntry(DrawerAppAction.AppInfo, stringResource(UiR.string.ui_menu_app_info), BricksIcons.Info))
    add(ItemMenuEntry(DrawerAppAction.AddToHome, stringResource(UiR.string.ui_menu_add_home), BricksIcons.Home))
    add(ItemMenuEntry(DrawerAppAction.AddToDock, stringResource(UiR.string.ui_menu_add_dock), BricksIcons.Dock))
    add(ItemMenuEntry(DrawerAppAction.Rename, stringResource(UiR.string.ui_menu_rename), BricksIcons.Label))
    add(ItemMenuEntry(DrawerAppAction.Hide, stringResource(UiR.string.ui_menu_hide), BricksIcons.EyeOff))
    if (!app.isSystem) {
        add(ItemMenuEntry(DrawerAppAction.Store, stringResource(UiR.string.ui_menu_store), BricksIcons.Store))
        add(
            ItemMenuEntry(
                DrawerAppAction.Uninstall,
                stringResource(UiR.string.ui_menu_uninstall),
                BricksIcons.Trash,
                destructive = true,
            ),
        )
    }
}

internal val DrawerItemMinWidth = 84.dp
