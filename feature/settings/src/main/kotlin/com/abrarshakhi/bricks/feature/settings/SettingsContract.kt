package com.abrarshakhi.bricks.feature.settings

import androidx.compose.runtime.Immutable
import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.model.AppSort
import com.abrarshakhi.bricks.core.domain.model.Backdrop
import com.abrarshakhi.bricks.core.domain.model.DrawerLayout
import com.abrarshakhi.bricks.core.domain.model.GestureAction
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.IconStyle
import com.abrarshakhi.bricks.core.domain.model.InkPalette
import com.abrarshakhi.bricks.core.domain.model.LauncherSettings
import com.abrarshakhi.bricks.core.domain.model.PaperTexture
import com.abrarshakhi.bricks.core.domain.model.ThemeMode

@Immutable
data class SettingsState(
    val loaded: Boolean = false,
    val settings: LauncherSettings = LauncherSettings(),
    val grid: GridSize = GridSize(4, 5),
    val isDefaultLauncher: Boolean = true,
    val lockScreenReady: Boolean = false,
    val notificationAccess: Boolean = false,
    val hiddenCount: Int = 0,
)

sealed interface SettingsIntent {
    data object Resumed : SettingsIntent
    data class SetThemeMode(val mode: ThemeMode) : SettingsIntent
    data class SetPalette(val palette: InkPalette) : SettingsIntent
    data class SetIconStyle(val style: IconStyle) : SettingsIntent
    data class SetIconScale(val scale: Float) : SettingsIntent
    data class SetBackdrop(val backdrop: Backdrop) : SettingsIntent
    data class SetWallpaperDim(val dim: Float) : SettingsIntent
    data class SetTexture(val texture: PaperTexture) : SettingsIntent
    data class SetGrid(val columns: Int, val rows: Int) : SettingsIntent
    data class SetShowLabels(val enabled: Boolean) : SettingsIntent
    data class SetShowGlance(val enabled: Boolean) : SettingsIntent
    data class SetShowSearchBar(val enabled: Boolean) : SettingsIntent
    data class SetDockEnabled(val enabled: Boolean) : SettingsIntent
    data class SetDockSlots(val slots: Int) : SettingsIntent
    data class SetAutoAddApps(val enabled: Boolean) : SettingsIntent
    data class SetLayoutLocked(val locked: Boolean) : SettingsIntent
    data class SetWallpaperScrolling(val enabled: Boolean) : SettingsIntent
    data class SetNotificationDots(val enabled: Boolean) : SettingsIntent
    data class SetDrawerLayout(val layout: DrawerLayout) : SettingsIntent
    data class SetDrawerSort(val sort: AppSort) : SettingsIntent
    data class SetDrawerLabels(val enabled: Boolean) : SettingsIntent
    data class SetSuggestions(val enabled: Boolean) : SettingsIntent
    data class SetDrawerIndex(val enabled: Boolean) : SettingsIntent
    data class SetAutoFocusSearch(val enabled: Boolean) : SettingsIntent
    data class SetSwipeUp(val action: GestureAction) : SettingsIntent
    data class SetSwipeDown(val action: GestureAction) : SettingsIntent
    data class SetDoubleTap(val action: GestureAction) : SettingsIntent
    data object OpenWallpaperPicker : SettingsIntent
    data object OpenNotificationAccess : SettingsIntent
    data object OpenLockScreenSetup : SettingsIntent
    data object SetDefaultLauncher : SettingsIntent
}

sealed interface SettingsEffect

@Immutable
data class HiddenAppsState(
    val loaded: Boolean = false,
    val apps: List<App> = emptyList(),
    val iconStyle: IconStyle = IconStyle.Sticker,
)

sealed interface HiddenAppsIntent {
    data class Unhide(val app: App) : HiddenAppsIntent
    data object UnhideAll : HiddenAppsIntent
}

sealed interface HiddenAppsEffect
