package com.abrarshakhi.bricks.feature.settings.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.component.EmptyState
import com.abrarshakhi.bricks.core.designsystem.component.SectionHeader
import com.abrarshakhi.bricks.core.designsystem.component.SettingChoiceRow
import com.abrarshakhi.bricks.core.designsystem.component.SettingNavigationRow
import com.abrarshakhi.bricks.core.designsystem.component.SettingSliderRow
import com.abrarshakhi.bricks.core.designsystem.component.SettingStepperRow
import com.abrarshakhi.bricks.core.designsystem.component.SettingSwitchRow
import com.abrarshakhi.bricks.core.designsystem.component.halftone
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.domain.model.AppSort
import com.abrarshakhi.bricks.core.domain.model.AppearanceSettings
import com.abrarshakhi.bricks.core.domain.model.Backdrop
import com.abrarshakhi.bricks.core.domain.model.DrawerLayout
import com.abrarshakhi.bricks.core.domain.model.GestureAction
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.HomeSettings
import com.abrarshakhi.bricks.core.domain.model.IconStyle
import com.abrarshakhi.bricks.core.domain.model.InkPalette
import com.abrarshakhi.bricks.core.domain.model.PaperTexture
import com.abrarshakhi.bricks.core.domain.model.ThemeMode
import com.abrarshakhi.bricks.core.ui.component.AppIconTile
import com.abrarshakhi.bricks.core.ui.icon.rememberAppIcon
import com.abrarshakhi.bricks.feature.settings.HiddenAppsIntent
import com.abrarshakhi.bricks.feature.settings.HiddenAppsState
import com.abrarshakhi.bricks.feature.settings.R
import com.abrarshakhi.bricks.feature.settings.SettingsIntent
import com.abrarshakhi.bricks.feature.settings.SettingsState
import com.abrarshakhi.paperui.PuiButton
import com.abrarshakhi.paperui.PuiButtonDefaults
import com.abrarshakhi.paperui.PuiCard
import com.abrarshakhi.paperui.PuiListItem
import com.abrarshakhi.paperui.PuiOutlinedButton
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme
import com.abrarshakhi.bricks.core.ui.R as UiR
import kotlin.math.roundToInt

enum class SettingsPage { Appearance, Home, Drawer, Gestures, HiddenApps, About }

@Composable
internal fun SettingsRootScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onOpen: (SettingsPage) -> Unit,
    onBack: () -> Unit,
) {
    val settings = state.settings
    SettingsScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        item(key = "hero") { HeroCard(state, onIntent) }
        item(key = "customize") { SectionHeader(stringResource(R.string.settings_section_customize), SettingsItemModifier) }
        item(key = "appearance") {
            SettingNavigationRow(
                title = stringResource(R.string.settings_appearance),
                subtitle = "${paletteLabel(settings.appearance.palette)} · ${iconStyleLabel(settings.appearance.iconStyle)}",
                icon = BricksIcons.Palette,
                onClick = { onOpen(SettingsPage.Appearance) },
                modifier = SettingsItemModifier,
            )
        }
        item(key = "home") {
            SettingNavigationRow(
                title = stringResource(R.string.settings_home),
                subtitle = stringResource(R.string.settings_home_summary, state.grid.columns, state.grid.rows),
                icon = BricksIcons.Home,
                onClick = { onOpen(SettingsPage.Home) },
                modifier = SettingsItemModifier,
            )
        }
        item(key = "drawer") {
            SettingNavigationRow(
                title = stringResource(R.string.settings_drawer),
                subtitle = "${drawerLayoutLabel(settings.drawer.layout)} · ${sortLabel(settings.drawer.sort)}",
                icon = BricksIcons.Apps,
                onClick = { onOpen(SettingsPage.Drawer) },
                modifier = SettingsItemModifier,
            )
        }
        item(key = "gestures") {
            SettingNavigationRow(
                title = stringResource(R.string.settings_gestures),
                subtitle = stringResource(
                    R.string.settings_gestures_summary,
                    gestureLabel(settings.gestures.swipeDown),
                    gestureLabel(settings.gestures.doubleTap),
                ),
                icon = BricksIcons.Gestures,
                onClick = { onOpen(SettingsPage.Gestures) },
                modifier = SettingsItemModifier,
            )
        }
        item(key = "apps") { SectionHeader(stringResource(R.string.settings_section_apps), SettingsItemModifier) }
        item(key = "hidden") {
            SettingNavigationRow(
                title = stringResource(R.string.settings_hidden_apps),
                subtitle = stringResource(R.string.settings_hidden_apps_summary),
                icon = BricksIcons.EyeOff,
                value = state.hiddenCount.toString(),
                onClick = { onOpen(SettingsPage.HiddenApps) },
                modifier = SettingsItemModifier,
            )
        }
        item(key = "more") { SectionHeader(stringResource(R.string.settings_section_more), SettingsItemModifier) }
        item(key = "about") {
            SettingNavigationRow(
                title = stringResource(R.string.settings_about),
                icon = BricksIcons.Info,
                onClick = { onOpen(SettingsPage.About) },
                modifier = SettingsItemModifier,
            )
        }
    }
}

@Composable
private fun HeroCard(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    PuiCard(
        modifier = SettingsItemModifier,
        color = PuiTheme.colors.primary,
        elevation = PuiTheme.elevation.level3,
    ) {
        Column(modifier = Modifier.fillMaxWidth().halftone(color = PuiTheme.colors.onPrimary, alpha = 0.12f)) {
            PuiText(
                text = stringResource(R.string.settings_brand),
                style = PuiTheme.typography.displaySmall,
                color = PuiTheme.colors.onPrimary,
            )
            PuiText(
                text = stringResource(R.string.settings_brand_tagline),
                style = PuiTheme.typography.bodyMedium,
                color = PuiTheme.colors.onPrimary,
            )
            if (!state.isDefaultLauncher) {
                Spacer(Modifier.height(PuiTheme.spacing.md))
                PuiButton(
                    onClick = { onIntent(SettingsIntent.SetDefaultLauncher) },
                    colors = PuiButtonDefaults.accent(),
                ) { PuiText(stringResource(R.string.settings_set_default)) }
            }
        }
    }
}

@Composable
internal fun AppearanceScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    val appearance = state.settings.appearance
    var scale by remember(appearance.iconScale) { mutableFloatStateOf(appearance.iconScale) }
    var dim by remember(appearance.wallpaperDim) { mutableFloatStateOf(appearance.wallpaperDim) }
    SettingsScaffold(title = stringResource(R.string.settings_appearance), onBack = onBack) {
        item(key = "preview") {
            ThemePreview(
                palette = appearance.palette,
                themeMode = appearance.themeMode,
                iconStyle = appearance.iconStyle,
                texture = appearance.texture,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "theme") {
            SettingChoiceRow(
                title = stringResource(R.string.settings_theme),
                options = ThemeMode.entries,
                selected = appearance.themeMode,
                onSelect = { onIntent(SettingsIntent.SetThemeMode(it)) },
                label = { themeModeLabel(it) },
                icon = if (appearance.themeMode == ThemeMode.Dark) BricksIcons.Moon else BricksIcons.Sun,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "palette") {
            SettingChoiceRow(
                title = stringResource(R.string.settings_palette),
                subtitle = stringResource(R.string.settings_palette_summary),
                options = InkPalette.entries,
                selected = appearance.palette,
                onSelect = { onIntent(SettingsIntent.SetPalette(it)) },
                label = { paletteLabel(it) },
                icon = BricksIcons.Palette,
                leading = { PaletteSwatch(it) },
                modifier = SettingsItemModifier,
            )
        }
        item(key = "icons") {
            SettingChoiceRow(
                title = stringResource(R.string.settings_icons),
                subtitle = stringResource(R.string.settings_icons_summary),
                options = IconStyle.entries,
                selected = appearance.iconStyle,
                onSelect = { onIntent(SettingsIntent.SetIconStyle(it)) },
                label = { iconStyleLabel(it) },
                icon = BricksIcons.Star,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "scale") {
            SettingSliderRow(
                title = stringResource(R.string.settings_icon_size),
                value = scale,
                onValueChange = { scale = it },
                onValueChangeFinished = { onIntent(SettingsIntent.SetIconScale(scale)) },
                valueLabel = "${(scale * 100).roundToInt()}%",
                valueRange = AppearanceSettings.MIN_ICON_SCALE..AppearanceSettings.MAX_ICON_SCALE,
                steps = 8,
                icon = BricksIcons.Resize,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "backdrop-header") { SectionHeader(stringResource(R.string.settings_section_backdrop), SettingsItemModifier) }
        item(key = "backdrop") {
            SettingChoiceRow(
                title = stringResource(R.string.settings_backdrop),
                options = Backdrop.entries,
                selected = appearance.backdrop,
                onSelect = { onIntent(SettingsIntent.SetBackdrop(it)) },
                label = { backdropLabel(it) },
                icon = BricksIcons.Layers,
                modifier = SettingsItemModifier,
            )
        }
        if (appearance.backdrop == Backdrop.Wallpaper) {
            item(key = "dim") {
                SettingSliderRow(
                    title = stringResource(R.string.settings_wallpaper_dim),
                    value = dim,
                    onValueChange = { dim = it },
                    onValueChangeFinished = { onIntent(SettingsIntent.SetWallpaperDim(dim)) },
                    valueLabel = "${(dim * 100).roundToInt()}%",
                    valueRange = 0f..AppearanceSettings.MAX_WALLPAPER_DIM,
                    steps = 6,
                    icon = BricksIcons.Moon,
                    modifier = SettingsItemModifier,
                )
            }
            item(key = "wallpaper") {
                SettingNavigationRow(
                    title = stringResource(R.string.settings_change_wallpaper),
                    icon = BricksIcons.Wallpaper,
                    onClick = { onIntent(SettingsIntent.OpenWallpaperPicker) },
                    modifier = SettingsItemModifier,
                )
            }
        }
        item(key = "texture") {
            SettingChoiceRow(
                title = stringResource(R.string.settings_texture),
                subtitle = stringResource(R.string.settings_texture_summary),
                options = PaperTexture.entries,
                selected = appearance.texture,
                onSelect = { onIntent(SettingsIntent.SetTexture(it)) },
                label = { textureLabel(it) },
                icon = BricksIcons.Texture,
                modifier = SettingsItemModifier,
            )
        }
    }
}

@Composable
internal fun HomeSettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    val home = state.settings.home
    SettingsScaffold(title = stringResource(R.string.settings_home), onBack = onBack) {
        item(key = "grid-header") { SectionHeader(stringResource(R.string.settings_section_grid), SettingsItemModifier) }
        item(key = "columns") {
            SettingStepperRow(
                title = stringResource(R.string.settings_columns),
                value = state.grid.columns,
                range = GridSize.MIN_COLUMNS..GridSize.MAX_COLUMNS,
                onValueChange = { onIntent(SettingsIntent.SetGrid(it, state.grid.rows)) },
                icon = BricksIcons.Grid,
                decreaseLabel = stringResource(R.string.settings_decrease),
                increaseLabel = stringResource(R.string.settings_increase),
                modifier = SettingsItemModifier,
            )
        }
        item(key = "rows") {
            SettingStepperRow(
                title = stringResource(R.string.settings_rows),
                value = state.grid.rows,
                range = GridSize.MIN_ROWS..GridSize.MAX_ROWS,
                onValueChange = { onIntent(SettingsIntent.SetGrid(state.grid.columns, it)) },
                icon = BricksIcons.Grid,
                decreaseLabel = stringResource(R.string.settings_decrease),
                increaseLabel = stringResource(R.string.settings_increase),
                modifier = SettingsItemModifier,
            )
        }
        item(key = "labels") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_labels),
                checked = home.showLabels,
                onCheckedChange = { onIntent(SettingsIntent.SetShowLabels(it)) },
                icon = BricksIcons.Letter,
                stateOn = stringResource(UiR.string.ui_state_on),
                stateOff = stringResource(UiR.string.ui_state_off),
                modifier = SettingsItemModifier,
            )
        }
        item(key = "chrome-header") { SectionHeader(stringResource(R.string.settings_section_chrome), SettingsItemModifier) }
        item(key = "glance") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_glance),
                subtitle = stringResource(R.string.settings_show_glance_summary),
                checked = home.showGlance,
                onCheckedChange = { onIntent(SettingsIntent.SetShowGlance(it)) },
                icon = BricksIcons.Clock,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "search") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_search),
                checked = home.showSearchBar,
                onCheckedChange = { onIntent(SettingsIntent.SetShowSearchBar(it)) },
                icon = BricksIcons.Search,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "dock") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_dock),
                checked = home.dockEnabled,
                onCheckedChange = { onIntent(SettingsIntent.SetDockEnabled(it)) },
                icon = BricksIcons.Dock,
                modifier = SettingsItemModifier,
            )
        }
        if (home.dockEnabled) {
            item(key = "dock-slots") {
                SettingStepperRow(
                    title = stringResource(R.string.settings_dock_slots),
                    value = home.dockSlots,
                    range = HomeSettings.MIN_DOCK_SLOTS..HomeSettings.MAX_DOCK_SLOTS,
                    onValueChange = { onIntent(SettingsIntent.SetDockSlots(it)) },
                    icon = BricksIcons.Dock,
                    decreaseLabel = stringResource(R.string.settings_decrease),
                    increaseLabel = stringResource(R.string.settings_increase),
                    modifier = SettingsItemModifier,
                )
            }
        }
        item(key = "behaviour-header") { SectionHeader(stringResource(R.string.settings_section_behaviour), SettingsItemModifier) }
        item(key = "auto-add") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_auto_add),
                subtitle = stringResource(R.string.settings_auto_add_summary),
                checked = home.autoAddNewApps,
                onCheckedChange = { onIntent(SettingsIntent.SetAutoAddApps(it)) },
                icon = BricksIcons.Plus,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "lock") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_lock_layout),
                subtitle = stringResource(R.string.settings_lock_layout_summary),
                checked = home.layoutLocked,
                onCheckedChange = { onIntent(SettingsIntent.SetLayoutLocked(it)) },
                icon = BricksIcons.Lock,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "scroll") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_wallpaper_scroll),
                subtitle = stringResource(R.string.settings_wallpaper_scroll_summary),
                checked = home.wallpaperScrolling,
                onCheckedChange = { onIntent(SettingsIntent.SetWallpaperScrolling(it)) },
                icon = BricksIcons.Wallpaper,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "dots") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_notification_dots),
                subtitle = stringResource(
                    if (state.notificationAccess) R.string.settings_dots_granted else R.string.settings_dots_needs_access,
                ),
                checked = home.notificationDots,
                onCheckedChange = { onIntent(SettingsIntent.SetNotificationDots(it)) },
                icon = BricksIcons.Bell,
                modifier = SettingsItemModifier,
            )
        }
        if (home.notificationDots && !state.notificationAccess) {
            item(key = "dots-access") {
                SettingNavigationRow(
                    title = stringResource(R.string.settings_grant_access),
                    icon = BricksIcons.Shield,
                    onClick = { onIntent(SettingsIntent.OpenNotificationAccess) },
                    modifier = SettingsItemModifier,
                )
            }
        }
    }
}

@Composable
internal fun DrawerSettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    val drawer = state.settings.drawer
    SettingsScaffold(title = stringResource(R.string.settings_drawer), onBack = onBack) {
        item(key = "layout") {
            SettingChoiceRow(
                title = stringResource(R.string.settings_drawer_layout),
                options = DrawerLayout.entries,
                selected = drawer.layout,
                onSelect = { onIntent(SettingsIntent.SetDrawerLayout(it)) },
                label = { drawerLayoutLabel(it) },
                icon = if (drawer.layout == DrawerLayout.List) BricksIcons.ListView else BricksIcons.Apps,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "sort") {
            SettingChoiceRow(
                title = stringResource(R.string.settings_drawer_sort),
                options = AppSort.entries,
                selected = drawer.sort,
                onSelect = { onIntent(SettingsIntent.SetDrawerSort(it)) },
                label = { sortLabel(it) },
                icon = BricksIcons.Sort,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "labels") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_labels),
                checked = drawer.showLabels,
                onCheckedChange = { onIntent(SettingsIntent.SetDrawerLabels(it)) },
                icon = BricksIcons.Letter,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "suggestions") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_suggestions),
                subtitle = stringResource(R.string.settings_suggestions_summary),
                checked = drawer.showSuggestions,
                onCheckedChange = { onIntent(SettingsIntent.SetSuggestions(it)) },
                icon = BricksIcons.Sparkle,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "index") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_index),
                subtitle = stringResource(R.string.settings_index_summary),
                checked = drawer.showIndex,
                onCheckedChange = { onIntent(SettingsIntent.SetDrawerIndex(it)) },
                icon = BricksIcons.Letter,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "focus") {
            SettingSwitchRow(
                title = stringResource(R.string.settings_auto_keyboard),
                subtitle = stringResource(R.string.settings_auto_keyboard_summary),
                checked = drawer.autoFocusSearch,
                onCheckedChange = { onIntent(SettingsIntent.SetAutoFocusSearch(it)) },
                icon = BricksIcons.Search,
                modifier = SettingsItemModifier,
            )
        }
    }
}

@Composable
internal fun GestureSettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    val gestures = state.settings.gestures
    val needsLock = gestures.doubleTap == GestureAction.LockScreen || gestures.swipeDown == GestureAction.LockScreen
    SettingsScaffold(title = stringResource(R.string.settings_gestures), onBack = onBack) {
        item(key = "swipe-up") {
            SettingChoiceRow(
                title = stringResource(R.string.settings_swipe_up),
                options = listOf(GestureAction.OpenDrawer, GestureAction.OpenSearch),
                selected = gestures.swipeUp,
                onSelect = { onIntent(SettingsIntent.SetSwipeUp(it)) },
                label = { gestureLabel(it) },
                icon = BricksIcons.ArrowUp,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "swipe-down") {
            SettingChoiceRow(
                title = stringResource(R.string.settings_swipe_down),
                options = listOf(
                    GestureAction.ExpandNotifications,
                    GestureAction.ExpandQuickSettings,
                    GestureAction.OpenSearch,
                    GestureAction.OpenDrawer,
                    GestureAction.LockScreen,
                    GestureAction.None,
                ),
                selected = gestures.swipeDown,
                onSelect = { onIntent(SettingsIntent.SetSwipeDown(it)) },
                label = { gestureLabel(it) },
                icon = BricksIcons.Gestures,
                modifier = SettingsItemModifier,
            )
        }
        item(key = "double-tap") {
            SettingChoiceRow(
                title = stringResource(R.string.settings_double_tap),
                options = listOf(
                    GestureAction.LockScreen,
                    GestureAction.ExpandNotifications,
                    GestureAction.OpenDrawer,
                    GestureAction.OpenSettings,
                    GestureAction.None,
                ),
                selected = gestures.doubleTap,
                onSelect = { onIntent(SettingsIntent.SetDoubleTap(it)) },
                label = { gestureLabel(it) },
                icon = BricksIcons.Lock,
                modifier = SettingsItemModifier,
            )
        }
        if (needsLock) {
            item(key = "lock-status") {
                PuiCard(
                    modifier = SettingsItemModifier,
                    color = if (state.lockScreenReady) PuiTheme.colors.surface else PuiTheme.colors.accent,
                    elevation = PuiTheme.elevation.level2,
                ) {
                    PuiText(
                        text = stringResource(
                            if (state.lockScreenReady) R.string.settings_lock_ready else R.string.settings_lock_needed,
                        ),
                        style = PuiTheme.typography.bodyMedium,
                    )
                    if (!state.lockScreenReady) {
                        Spacer(Modifier.height(PuiTheme.spacing.md))
                        PuiButton(onClick = { onIntent(SettingsIntent.OpenLockScreenSetup) }) {
                            PuiText(stringResource(R.string.settings_lock_enable))
                        }
                    }
                }
            }
        }
        item(key = "hint") {
            PuiText(
                text = stringResource(R.string.settings_gestures_hint),
                style = PuiTheme.typography.bodySmall,
                color = PuiTheme.colors.onSurfaceMuted,
                modifier = SettingsItemModifier.padding(horizontal = PuiTheme.spacing.xs),
            )
        }
    }
}

@Composable
internal fun HiddenAppsScreen(
    state: HiddenAppsState,
    iconStyle: IconStyle,
    onIntent: (HiddenAppsIntent) -> Unit,
    onBack: () -> Unit,
) {
    SettingsScaffold(title = stringResource(R.string.settings_hidden_apps), onBack = onBack) {
        if (state.loaded && state.apps.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.settings_hidden_empty_title),
                    message = stringResource(R.string.settings_hidden_empty_message),
                    icon = BricksIcons.Eye,
                    modifier = SettingsItemModifier,
                )
            }
        } else {
            item(key = "summary") {
                Row(
                    modifier = SettingsItemModifier,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PuiText(
                        text = stringResource(R.string.settings_hidden_summary),
                        style = PuiTheme.typography.bodyMedium,
                        color = PuiTheme.colors.onSurfaceMuted,
                        modifier = Modifier.weight(1f),
                    )
                    PuiOutlinedButton(
                        onClick = { onIntent(HiddenAppsIntent.UnhideAll) },
                        contentPadding = PuiButtonDefaults.CompactContentPadding,
                    ) { PuiText(stringResource(R.string.settings_hidden_show_all)) }
                }
            }
            items(state.apps, key = { it.key.flattened }) { app ->
                PuiListItem(
                    headline = { PuiText(app.label, maxLines = 1) },
                    supporting = if (app.label != app.systemLabel) {
                        { PuiText(app.systemLabel, maxLines = 1) }
                    } else {
                        null
                    },
                    leading = {
                        AppIconTile(
                            icon = rememberAppIcon(app.key, 40.dp, iconStyle),
                            style = iconStyle,
                            size = 40.dp,
                        )
                    },
                    trailing = {
                        PuiOutlinedButton(
                            onClick = { onIntent(HiddenAppsIntent.Unhide(app)) },
                            contentPadding = PuiButtonDefaults.CompactContentPadding,
                        ) { PuiText(stringResource(UiR.string.ui_menu_unhide)) }
                    },
                    modifier = SettingsItemModifier,
                )
            }
        }
    }
}

@Composable
internal fun AboutScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            val manager = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                manager.getPackageInfo(context.packageName, 0)
            }
            info.versionName
        }.getOrNull().orEmpty()
    }
    SettingsScaffold(title = stringResource(R.string.settings_about), onBack = onBack) {
        item(key = "brand") {
            PuiCard(modifier = SettingsItemModifier, elevation = PuiTheme.elevation.level3) {
                PuiText(stringResource(R.string.settings_brand), style = PuiTheme.typography.displayMedium, color = PuiTheme.colors.primary)
                PuiText(stringResource(R.string.settings_version, version), style = PuiTheme.typography.labelLarge)
                Spacer(Modifier.height(PuiTheme.spacing.sm))
                PuiText(
                    text = stringResource(R.string.settings_about_body),
                    style = PuiTheme.typography.bodyMedium,
                    color = PuiTheme.colors.onSurfaceMuted,
                )
            }
        }
        item(key = "default") {
            SettingNavigationRow(
                title = stringResource(R.string.settings_default_launcher),
                subtitle = stringResource(
                    if (state.isDefaultLauncher) R.string.settings_default_yes else R.string.settings_default_no,
                ),
                icon = BricksIcons.Home,
                onClick = { onIntent(SettingsIntent.SetDefaultLauncher) },
                modifier = SettingsItemModifier,
            )
        }
        item(key = "credits") {
            Column(modifier = SettingsItemModifier, verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm)) {
                SectionHeader(stringResource(R.string.settings_credits))
                PuiText(stringResource(R.string.settings_credit_paperui), style = PuiTheme.typography.bodySmall)
                PuiText(stringResource(R.string.settings_credit_font), style = PuiTheme.typography.bodySmall)
            }
        }
        item(key = "spacer") { Spacer(Modifier.fillMaxWidth().height(PuiTheme.spacing.xl)) }
    }
}

@Composable
internal fun SettingsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PuiTheme.spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            title = stringResource(R.string.settings_pick_title),
            message = stringResource(R.string.settings_pick_message),
            icon = BricksIcons.Settings,
        )
    }
}
