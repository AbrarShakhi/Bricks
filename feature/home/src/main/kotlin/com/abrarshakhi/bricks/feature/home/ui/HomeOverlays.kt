package com.abrarshakhi.bricks.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.component.SettingStepperRow
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.ui.component.ConfirmDialog
import com.abrarshakhi.bricks.core.ui.component.TextInputDialog
import com.abrarshakhi.bricks.feature.home.HomeIntent
import com.abrarshakhi.bricks.feature.home.HomeMenuAction
import com.abrarshakhi.bricks.feature.home.HomeState
import com.abrarshakhi.bricks.feature.home.HomeTile
import com.abrarshakhi.bricks.feature.home.R
import com.abrarshakhi.bricks.feature.home.TileAction
import com.abrarshakhi.bricks.feature.home.TileMenu
import com.abrarshakhi.paperui.PuiButton
import com.abrarshakhi.paperui.PuiButtonDefaults
import com.abrarshakhi.paperui.PuiDropdownMenu
import com.abrarshakhi.paperui.PuiDropdownMenuItem
import com.abrarshakhi.paperui.PuiHorizontalDivider
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiIconButton
import com.abrarshakhi.paperui.PuiModalBottomSheet
import com.abrarshakhi.paperui.PuiOutlinedButton
import com.abrarshakhi.paperui.PuiScrim
import com.abrarshakhi.paperui.PuiSurface
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme
import com.abrarshakhi.paperui.puiHalftone
import kotlin.math.roundToInt
import com.abrarshakhi.bricks.core.ui.R as UiR

@Composable
internal fun FolderOverlay(
    folder: HomeTile.Folder?,
    environment: TileEnvironment,
    menu: TileMenu?,
    maxHeight: Dp,
    onIntent: (HomeIntent) -> Unit,
) {
    var lastFolder by remember { mutableStateOf(folder) }
    if (folder != null) lastFolder = folder
    AnimatedVisibility(
        visible = folder != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PuiScrim(visible = true, onDismiss = { onIntent(HomeIntent.CloseFolder) })
            val shown = lastFolder
            if (shown != null) {
                AnimatedVisibility(
                    visible = folder != null,
                    enter = scaleIn(initialScale = 0.85f) + fadeIn(),
                    exit = scaleOut(targetScale = 0.9f) + fadeOut(),
                ) {
                    FolderPanel(shown, environment, menu, maxHeight, onIntent)
                }
            }
        }
    }
}

@Composable
private fun FolderPanel(
    folder: HomeTile.Folder,
    environment: TileEnvironment,
    menu: TileMenu?,
    maxHeight: Dp,
    onIntent: (HomeIntent) -> Unit,
) {
    val columns = when {
        folder.children.size <= 4 -> 2
        folder.children.size <= 9 -> 3
        else -> 4
    }
    val cellHeight = environment.iconSize + 44.dp
    val title = folder.title.ifBlank { stringResource(R.string.home_folder_default) }
    PuiSurface(
        modifier = Modifier
            .padding(PuiTheme.spacing.xl)
            .widthIn(min = 240.dp, max = 440.dp),
        shape = PuiTheme.shapes.large,
        borderWidth = PuiTheme.stroke.bold,
        elevation = PuiTheme.elevation.level4,
    ) {
        Column(modifier = Modifier.padding(PuiTheme.spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PuiText(
                    text = title,
                    style = PuiTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                        ) { onIntent(HomeIntent.TileMenuAction(folder.id, TileAction.RenameFolder)) },
                )
                PuiIconButton(
                    onClick = { onIntent(HomeIntent.TileMenuAction(folder.id, TileAction.RenameFolder)) },
                    size = 40.dp,
                ) { PuiIcon(BricksIcons.Edit, contentDescription = stringResource(UiR.string.ui_menu_rename), size = 20.dp) }
            }
            Spacer(Modifier.height(PuiTheme.spacing.sm))
            PuiHorizontalDivider(
                modifier = Modifier.puiHalftone(color = PuiTheme.colors.ink),
                thickness = PuiTheme.stroke.regular,
            )
            Spacer(Modifier.height(PuiTheme.spacing.lg))
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.heightIn(max = maxHeight * 0.6f),
                verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.xs),
            ) {
                items(folder.children, key = { it.id }) { child ->
                    HomeTileView(
                        tile = child,
                        environment = environment.copy(onWallpaper = false, showLabel = true),
                        menu = menu,
                        onIntent = onIntent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cellHeight),
                        inFolder = true,
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeOptionsMenu(
    visible: Boolean,
    anchor: Offset,
    onIntent: (HomeIntent) -> Unit,
) {
    if (!visible) return
    Box(
        modifier = Modifier
            .offset { IntOffset(anchor.x.roundToInt(), anchor.y.roundToInt()) }
            .size(1.dp),
    ) {
        PuiDropdownMenu(expanded = true, onDismissRequest = { onIntent(HomeIntent.DismissMenus) }) {
            HomeMenuItem(BricksIcons.Grid, stringResource(R.string.home_menu_edit)) {
                onIntent(HomeIntent.HomeMenu(HomeMenuAction.EditHome))
            }
            HomeMenuItem(BricksIcons.Widgets, stringResource(R.string.home_menu_widgets)) {
                onIntent(HomeIntent.HomeMenu(HomeMenuAction.Widgets))
            }
            HomeMenuItem(BricksIcons.Wallpaper, stringResource(R.string.home_menu_wallpaper)) {
                onIntent(HomeIntent.HomeMenu(HomeMenuAction.Wallpaper))
            }
            HomeMenuItem(BricksIcons.Settings, stringResource(R.string.home_menu_settings)) {
                onIntent(HomeIntent.HomeMenu(HomeMenuAction.Settings))
            }
        }
    }
}

@Composable
private fun HomeMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    PuiDropdownMenuItem(
        onClick = onClick,
        leading = { PuiIcon(icon, contentDescription = null, size = 22.dp) },
    ) { PuiText(label) }
}

@Composable
internal fun EditModeBar(onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    PuiSurface(
        modifier = modifier.fillMaxWidth(),
        shape = PuiTheme.shapes.medium,
        elevation = PuiTheme.elevation.level3,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PuiTheme.spacing.md, vertical = PuiTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm),
        ) {
            PuiText(
                text = stringResource(R.string.home_edit_title),
                style = PuiTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            PuiIconButton(onClick = { onIntent(HomeIntent.HomeMenu(HomeMenuAction.Widgets)) }, size = 40.dp) {
                PuiIcon(BricksIcons.Widgets, contentDescription = stringResource(R.string.home_menu_widgets), size = 20.dp)
            }
            PuiIconButton(onClick = { onIntent(HomeIntent.HomeMenu(HomeMenuAction.Wallpaper)) }, size = 40.dp) {
                PuiIcon(BricksIcons.Wallpaper, contentDescription = stringResource(R.string.home_menu_wallpaper), size = 20.dp)
            }
            PuiIconButton(onClick = { onIntent(HomeIntent.HomeMenu(HomeMenuAction.Settings)) }, size = 40.dp) {
                PuiIcon(BricksIcons.Settings, contentDescription = stringResource(R.string.home_menu_settings), size = 20.dp)
            }
            PuiButton(
                onClick = { onIntent(HomeIntent.ExitEditMode) },
                contentPadding = PuiButtonDefaults.CompactContentPadding,
            ) { PuiText(stringResource(UiR.string.ui_action_done)) }
        }
    }
}

@Composable
internal fun EditPageActions(
    page: Int,
    canRemove: Boolean,
    canAdd: Boolean,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md, Alignment.CenterHorizontally),
    ) {
        PuiOutlinedButton(
            onClick = { onIntent(HomeIntent.AddPage) },
            enabled = canAdd,
            colors = PuiButtonDefaults.outlined(content = PuiTheme.colors.onSurface),
            contentPadding = PuiButtonDefaults.CompactContentPadding,
            modifier = Modifier,
        ) {
            PuiIcon(BricksIcons.PageAdd, contentDescription = null, size = 18.dp)
            Spacer(Modifier.size(PuiTheme.spacing.xs))
            PuiText(stringResource(R.string.home_edit_add_page))
        }
        PuiOutlinedButton(
            onClick = { onIntent(HomeIntent.RequestRemovePage(page)) },
            enabled = canRemove,
            colors = PuiButtonDefaults.outlined(content = PuiTheme.colors.danger),
            contentPadding = PuiButtonDefaults.CompactContentPadding,
        ) {
            PuiIcon(BricksIcons.Trash, contentDescription = null, size = 18.dp)
            Spacer(Modifier.size(PuiTheme.spacing.xs))
            PuiText(stringResource(R.string.home_edit_remove_page))
        }
    }
}

@Composable
internal fun HomeDialogs(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    val renameFolder = state.renameFolderId?.let { state.findTile(it) as? HomeTile.Folder }
    if (renameFolder != null) {
        TextInputDialog(
            title = stringResource(R.string.home_folder_rename_title),
            initialValue = renameFolder.title,
            placeholder = stringResource(R.string.home_folder_default),
            onDismiss = { onIntent(HomeIntent.DismissRename) },
            onConfirm = { onIntent(HomeIntent.RenameFolder(renameFolder.id, it)) },
        )
    }
    val renameApp = state.renameApp
    if (renameApp != null) {
        TextInputDialog(
            title = stringResource(UiR.string.ui_rename_app_title),
            initialValue = renameApp.label,
            placeholder = renameApp.systemLabel,
            onDismiss = { onIntent(HomeIntent.DismissRename) },
            onConfirm = { onIntent(HomeIntent.RenameApp(renameApp.app, it)) },
            resetLabel = stringResource(UiR.string.ui_rename_reset),
            onReset = { onIntent(HomeIntent.RenameApp(renameApp.app, null)) },
        )
    }
    if (state.confirmRemovePage != null) {
        ConfirmDialog(
            title = stringResource(R.string.home_remove_page_title),
            message = stringResource(R.string.home_remove_page_message),
            confirmLabel = stringResource(R.string.home_remove_page_confirm),
            onDismiss = { onIntent(HomeIntent.DismissRemovePage) },
            onConfirm = { onIntent(HomeIntent.ConfirmRemovePage) },
            destructive = true,
        )
    }
    val resize = state.resize
    if (resize != null) {
        PuiModalBottomSheet(onDismissRequest = { onIntent(HomeIntent.DismissResize) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md),
            ) {
                PuiText(stringResource(R.string.home_resize_title), style = PuiTheme.typography.headlineSmall)
                SettingStepperRow(
                    title = stringResource(R.string.home_resize_width),
                    value = resize.span.width,
                    range = resize.minSpan.width..resize.maxSpan.width,
                    onValueChange = { onIntent(HomeIntent.ResizeWidget(resize.tileId, resize.span.copy(width = it))) },
                    icon = BricksIcons.Resize,
                )
                SettingStepperRow(
                    title = stringResource(R.string.home_resize_height),
                    value = resize.span.height,
                    range = resize.minSpan.height..resize.maxSpan.height,
                    onValueChange = { onIntent(HomeIntent.ResizeWidget(resize.tileId, resize.span.copy(height = it))) },
                    icon = BricksIcons.Resize,
                )
                PuiButton(
                    onClick = { onIntent(HomeIntent.DismissResize) },
                    modifier = Modifier.fillMaxWidth(),
                ) { PuiText(stringResource(UiR.string.ui_action_done)) }
            }
        }
    }
}
