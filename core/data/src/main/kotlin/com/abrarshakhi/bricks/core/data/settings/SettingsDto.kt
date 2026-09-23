package com.abrarshakhi.bricks.core.data.settings

import com.abrarshakhi.bricks.core.data.store.enumOrDefault
import com.abrarshakhi.bricks.core.domain.model.AppearanceSettings
import com.abrarshakhi.bricks.core.domain.model.DrawerSettings
import com.abrarshakhi.bricks.core.domain.model.GestureSettings
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.HomeSettings
import com.abrarshakhi.bricks.core.domain.model.LauncherSettings
import kotlinx.serialization.Serializable

@Serializable
internal data class SettingsDto(
    val version: Int = 1,
    val themeMode: String? = null,
    val palette: String? = null,
    val iconStyle: String? = null,
    val iconScale: Float? = null,
    val backdrop: String? = null,
    val wallpaperDim: Float? = null,
    val texture: String? = null,
    val gridColumns: Int? = null,
    val gridRows: Int? = null,
    val showLabels: Boolean? = null,
    val showGlance: Boolean? = null,
    val showSearchBar: Boolean? = null,
    val dockEnabled: Boolean? = null,
    val dockSlots: Int? = null,
    val autoAddNewApps: Boolean? = null,
    val layoutLocked: Boolean? = null,
    val wallpaperScrolling: Boolean? = null,
    val notificationDots: Boolean? = null,
    val drawerLayout: String? = null,
    val drawerSort: String? = null,
    val drawerSuggestions: Boolean? = null,
    val drawerLabels: Boolean? = null,
    val drawerAutoFocus: Boolean? = null,
    val drawerIndex: Boolean? = null,
    val swipeUp: String? = null,
    val swipeDown: String? = null,
    val doubleTap: String? = null,
)

internal fun SettingsDto.toDomain(): LauncherSettings {
    val appearanceDefaults = AppearanceSettings()
    val homeDefaults = HomeSettings()
    val drawerDefaults = DrawerSettings()
    val gestureDefaults = GestureSettings()
    return LauncherSettings(
        appearance = AppearanceSettings(
            themeMode = enumOrDefault(themeMode, appearanceDefaults.themeMode),
            palette = enumOrDefault(palette, appearanceDefaults.palette),
            iconStyle = enumOrDefault(iconStyle, appearanceDefaults.iconStyle),
            iconScale = (iconScale ?: appearanceDefaults.iconScale)
                .coerceIn(AppearanceSettings.MIN_ICON_SCALE, AppearanceSettings.MAX_ICON_SCALE),
            backdrop = enumOrDefault(backdrop, appearanceDefaults.backdrop),
            wallpaperDim = (wallpaperDim ?: appearanceDefaults.wallpaperDim)
                .coerceIn(0f, AppearanceSettings.MAX_WALLPAPER_DIM),
            texture = enumOrDefault(texture, appearanceDefaults.texture),
        ),
        home = HomeSettings(
            grid = if (gridColumns != null && gridRows != null) GridSize.of(gridColumns, gridRows) else null,
            showLabels = showLabels ?: homeDefaults.showLabels,
            showGlance = showGlance ?: homeDefaults.showGlance,
            showSearchBar = showSearchBar ?: homeDefaults.showSearchBar,
            dockEnabled = dockEnabled ?: homeDefaults.dockEnabled,
            dockSlots = (dockSlots ?: homeDefaults.dockSlots)
                .coerceIn(HomeSettings.MIN_DOCK_SLOTS, HomeSettings.MAX_DOCK_SLOTS),
            autoAddNewApps = autoAddNewApps ?: homeDefaults.autoAddNewApps,
            layoutLocked = layoutLocked ?: homeDefaults.layoutLocked,
            wallpaperScrolling = wallpaperScrolling ?: homeDefaults.wallpaperScrolling,
            notificationDots = notificationDots ?: homeDefaults.notificationDots,
        ),
        drawer = DrawerSettings(
            layout = enumOrDefault(drawerLayout, drawerDefaults.layout),
            sort = enumOrDefault(drawerSort, drawerDefaults.sort),
            showSuggestions = drawerSuggestions ?: drawerDefaults.showSuggestions,
            showLabels = drawerLabels ?: drawerDefaults.showLabels,
            autoFocusSearch = drawerAutoFocus ?: drawerDefaults.autoFocusSearch,
            showIndex = drawerIndex ?: drawerDefaults.showIndex,
        ),
        gestures = GestureSettings(
            swipeUp = enumOrDefault(swipeUp, gestureDefaults.swipeUp),
            swipeDown = enumOrDefault(swipeDown, gestureDefaults.swipeDown),
            doubleTap = enumOrDefault(doubleTap, gestureDefaults.doubleTap),
        ),
    )
}

internal fun LauncherSettings.toDto(): SettingsDto = SettingsDto(
    themeMode = appearance.themeMode.name,
    palette = appearance.palette.name,
    iconStyle = appearance.iconStyle.name,
    iconScale = appearance.iconScale,
    backdrop = appearance.backdrop.name,
    wallpaperDim = appearance.wallpaperDim,
    texture = appearance.texture.name,
    gridColumns = home.grid?.columns,
    gridRows = home.grid?.rows,
    showLabels = home.showLabels,
    showGlance = home.showGlance,
    showSearchBar = home.showSearchBar,
    dockEnabled = home.dockEnabled,
    dockSlots = home.dockSlots,
    autoAddNewApps = home.autoAddNewApps,
    layoutLocked = home.layoutLocked,
    wallpaperScrolling = home.wallpaperScrolling,
    notificationDots = home.notificationDots,
    drawerLayout = drawer.layout.name,
    drawerSort = drawer.sort.name,
    drawerSuggestions = drawer.showSuggestions,
    drawerLabels = drawer.showLabels,
    drawerAutoFocus = drawer.autoFocusSearch,
    drawerIndex = drawer.showIndex,
    swipeUp = gestures.swipeUp.name,
    swipeDown = gestures.swipeDown.name,
    doubleTap = gestures.doubleTap.name,
)
