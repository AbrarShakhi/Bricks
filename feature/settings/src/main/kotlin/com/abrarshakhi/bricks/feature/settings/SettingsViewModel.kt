package com.abrarshakhi.bricks.feature.settings

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.bricks.core.domain.model.AppearanceSettings
import com.abrarshakhi.bricks.core.domain.model.DrawerSettings
import com.abrarshakhi.bricks.core.domain.model.GestureSettings
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.HomeSettings
import com.abrarshakhi.bricks.core.domain.model.LauncherSettings
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveHiddenAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.SetAppHiddenUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ChangeDockSlotsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ChangeHomeGridUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ObserveSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.UpdateSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveNotificationAccessUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.SystemStatusUseCase
import com.abrarshakhi.bricks.core.ui.mvi.MviViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsUseCases(
    val observeSettings: ObserveSettingsUseCase,
    val updateSettings: UpdateSettingsUseCase,
    val changeGrid: ChangeHomeGridUseCase,
    val changeDockSlots: ChangeDockSlotsUseCase,
    val observeHidden: ObserveHiddenAppsUseCase,
    val observeNotificationAccess: ObserveNotificationAccessUseCase,
    val system: SystemStatusUseCase,
)

class SettingsViewModel(
    private val useCases: SettingsUseCases,
) : MviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState()) {

    init {
        viewModelScope.launch {
            combine(
                useCases.observeSettings(),
                useCases.observeHidden(),
                useCases.observeNotificationAccess(),
            ) { settings, hidden, access -> Triple(settings, hidden.size, access) }
                .collect { (settings, hiddenCount, access) ->
                    reduce {
                        copy(
                            loaded = true,
                            settings = settings,
                            grid = settings.home.grid ?: GridSize.of(4, 5),
                            hiddenCount = hiddenCount,
                            notificationAccess = access,
                        )
                    }
                }
        }
        refreshStatus()
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Resumed -> refreshStatus()
            is SettingsIntent.SetThemeMode -> appearance { copy(themeMode = intent.mode) }
            is SettingsIntent.SetPalette -> appearance { copy(palette = intent.palette) }
            is SettingsIntent.SetIconStyle -> appearance { copy(iconStyle = intent.style) }
            is SettingsIntent.SetIconScale -> appearance {
                copy(iconScale = intent.scale.coerceIn(AppearanceSettings.MIN_ICON_SCALE, AppearanceSettings.MAX_ICON_SCALE))
            }
            is SettingsIntent.SetBackdrop -> appearance { copy(backdrop = intent.backdrop) }
            is SettingsIntent.SetWallpaperDim -> appearance {
                copy(wallpaperDim = intent.dim.coerceIn(0f, AppearanceSettings.MAX_WALLPAPER_DIM))
            }
            is SettingsIntent.SetTexture -> appearance { copy(texture = intent.texture) }
            is SettingsIntent.SetGrid -> viewModelScope.launch { useCases.changeGrid(intent.columns, intent.rows) }
            is SettingsIntent.SetShowLabels -> home { copy(showLabels = intent.enabled) }
            is SettingsIntent.SetShowGlance -> home { copy(showGlance = intent.enabled) }
            is SettingsIntent.SetShowSearchBar -> home { copy(showSearchBar = intent.enabled) }
            is SettingsIntent.SetDockEnabled -> home { copy(dockEnabled = intent.enabled) }
            is SettingsIntent.SetDockSlots -> viewModelScope.launch { useCases.changeDockSlots(intent.slots) }
            is SettingsIntent.SetAutoAddApps -> home { copy(autoAddNewApps = intent.enabled) }
            is SettingsIntent.SetLayoutLocked -> home { copy(layoutLocked = intent.locked) }
            is SettingsIntent.SetWallpaperScrolling -> home { copy(wallpaperScrolling = intent.enabled) }
            is SettingsIntent.SetNotificationDots -> {
                home { copy(notificationDots = intent.enabled) }
                if (intent.enabled && !currentState.notificationAccess) useCases.system.openNotificationAccessSettings()
            }
            is SettingsIntent.SetDrawerLayout -> drawer { copy(layout = intent.layout) }
            is SettingsIntent.SetDrawerSort -> drawer { copy(sort = intent.sort) }
            is SettingsIntent.SetDrawerLabels -> drawer { copy(showLabels = intent.enabled) }
            is SettingsIntent.SetSuggestions -> drawer { copy(showSuggestions = intent.enabled) }
            is SettingsIntent.SetDrawerIndex -> drawer { copy(showIndex = intent.enabled) }
            is SettingsIntent.SetAutoFocusSearch -> drawer { copy(autoFocusSearch = intent.enabled) }
            is SettingsIntent.SetSwipeUp -> gestures { copy(swipeUp = intent.action) }
            is SettingsIntent.SetSwipeDown -> gestures { copy(swipeDown = intent.action) }
            is SettingsIntent.SetDoubleTap -> gestures { copy(doubleTap = intent.action) }
            SettingsIntent.OpenWallpaperPicker -> useCases.system.openWallpaperPicker()
            SettingsIntent.OpenNotificationAccess -> useCases.system.openNotificationAccessSettings()
            SettingsIntent.OpenLockScreenSetup -> useCases.system.openLockScreenSetup()
            SettingsIntent.SetDefaultLauncher -> useCases.system.openDefaultLauncherSettings()
        }
    }

    private fun refreshStatus() {
        reduce {
            copy(
                isDefaultLauncher = useCases.system.isDefaultLauncher,
                lockScreenReady = useCases.system.isLockScreenReady,
            )
        }
    }

    private fun appearance(transform: AppearanceSettings.() -> AppearanceSettings) = update {
        it.copy(appearance = it.appearance.transform())
    }

    private fun home(transform: HomeSettings.() -> HomeSettings) = update {
        it.copy(home = it.home.transform())
    }

    private fun drawer(transform: DrawerSettings.() -> DrawerSettings) = update {
        it.copy(drawer = it.drawer.transform())
    }

    private fun gestures(transform: GestureSettings.() -> GestureSettings) = update {
        it.copy(gestures = it.gestures.transform())
    }

    private fun update(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { useCases.updateSettings(transform) }
    }
}

class HiddenAppsViewModel(
    observeHidden: ObserveHiddenAppsUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val setHidden: SetAppHiddenUseCase,
) : MviViewModel<HiddenAppsState, HiddenAppsIntent, HiddenAppsEffect>(HiddenAppsState()) {

    init {
        viewModelScope.launch {
            combine(observeHidden(), observeSettings()) { apps, settings -> apps to settings.appearance.iconStyle }
                .collect { (apps, style) -> reduce { copy(loaded = true, apps = apps, iconStyle = style) } }
        }
    }

    override fun onIntent(intent: HiddenAppsIntent) {
        when (intent) {
            is HiddenAppsIntent.Unhide -> viewModelScope.launch { setHidden(intent.app.key, false) }
            HiddenAppsIntent.UnhideAll -> {
                val apps = currentState.apps
                viewModelScope.launch { apps.forEach { setHidden(it.key, false) } }
            }
        }
    }
}
