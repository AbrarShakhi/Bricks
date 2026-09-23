package com.abrarshakhi.bricks.feature.settings

import com.abrarshakhi.bricks.core.domain.model.AppearanceSettings
import com.abrarshakhi.bricks.core.domain.model.GestureAction
import com.abrarshakhi.bricks.core.domain.model.GridCell
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.HomeItem
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.model.InkPalette
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.model.ThemeMode
import com.abrarshakhi.bricks.core.domain.testing.FakeAppRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeHomeLayoutRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeNotificationBadgeRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeSettingsRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeSystemActions
import com.abrarshakhi.bricks.core.domain.testing.MainDispatcherRule
import com.abrarshakhi.bricks.core.domain.testing.SystemScreen
import com.abrarshakhi.bricks.core.domain.testing.testApp
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveHiddenAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.SetAppHiddenUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ChangeDockSlotsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ChangeHomeGridUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ObserveSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.UpdateSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveNotificationAccessUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.SystemStatusUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val appRepository = FakeAppRepository(
        apps = listOf(testApp("maps", hidden = true), testApp("chrome"), testApp("notes", hidden = true)),
    )
    private val settingsRepository = FakeSettingsRepository()
    private val layoutRepository = FakeHomeLayoutRepository()
    private val badges = FakeNotificationBadgeRepository(accessGranted = false)
    private val system = FakeSystemActions(isLockScreenReady = false, isDefaultLauncher = false)

    private fun viewModel() = SettingsViewModel(
        SettingsUseCases(
            observeSettings = ObserveSettingsUseCase(settingsRepository),
            updateSettings = UpdateSettingsUseCase(settingsRepository),
            changeGrid = ChangeHomeGridUseCase(settingsRepository, layoutRepository),
            changeDockSlots = ChangeDockSlotsUseCase(settingsRepository, layoutRepository),
            observeHidden = ObserveHiddenAppsUseCase(appRepository),
            observeNotificationAccess = ObserveNotificationAccessUseCase(badges),
            system = SystemStatusUseCase(system),
        ),
    )

    @Test
    fun `state reflects stored settings and device status`() {
        val state = viewModel().state.value

        assertTrue(state.loaded)
        assertEquals(GridSize.of(4, 5), state.grid)
        assertEquals(2, state.hiddenCount)
        assertFalse(state.isDefaultLauncher)
        assertFalse(state.notificationAccess)
    }

    @Test
    fun `resuming refreshes the device status`() {
        val viewModel = viewModel()
        system.isDefaultLauncher = true
        system.isLockScreenReady = true

        viewModel.onIntent(SettingsIntent.Resumed)

        assertTrue(viewModel.state.value.isDefaultLauncher)
        assertTrue(viewModel.state.value.lockScreenReady)
    }

    @Test
    fun `appearance choices are saved`() {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetThemeMode(ThemeMode.Dark))
        viewModel.onIntent(SettingsIntent.SetPalette(InkPalette.Grape))
        viewModel.onIntent(SettingsIntent.SetIconScale(4f))

        val appearance = settingsRepository.state.value.appearance
        assertEquals(ThemeMode.Dark, appearance.themeMode)
        assertEquals(InkPalette.Grape, appearance.palette)
        assertEquals(AppearanceSettings.MAX_ICON_SCALE, appearance.iconScale)
        assertEquals(appearance, viewModel.state.value.settings.appearance)
    }

    @Test
    fun `shrinking the grid moves items back inside it`() {
        layoutRepository.state.value = HomeLayout.Empty.copy(
            items = listOf(HomeItem.Application(1L, Placement.Desktop(0, GridCell(3, 4), GridSpan.Single), testApp("chrome").key)),
        )
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetGrid(columns = 3, rows = 3))

        assertEquals(GridSize.of(3, 3), viewModel.state.value.grid)
        val placement = layoutRepository.state.value.items.single().placement as Placement.Desktop
        assertTrue(GridSize.of(3, 3).contains(placement.cell, placement.span))
    }

    @Test
    fun `turning on notification dots asks for access when missing`() {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetNotificationDots(true))

        assertTrue(settingsRepository.state.value.home.notificationDots)
        assertEquals(listOf(SystemScreen.NotificationAccess), system.opened)
    }

    @Test
    fun `turning on notification dots with access skips the prompt`() {
        badges.accessState.value = true
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetNotificationDots(true))

        assertTrue(system.opened.isEmpty())
    }

    @Test
    fun `gesture choices are saved`() {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetDoubleTap(GestureAction.LockScreen))

        assertEquals(GestureAction.LockScreen, settingsRepository.state.value.gestures.doubleTap)
    }

    @Test
    fun `unhiding everything restores hidden apps`() {
        val viewModel = HiddenAppsViewModel(
            observeHidden = ObserveHiddenAppsUseCase(appRepository),
            observeSettings = ObserveSettingsUseCase(settingsRepository),
            setHidden = SetAppHiddenUseCase(appRepository),
        )
        assertEquals(listOf("Maps", "Notes"), viewModel.state.value.apps.map { it.label })

        viewModel.onIntent(HiddenAppsIntent.UnhideAll)

        assertTrue(viewModel.state.value.apps.isEmpty())
        assertTrue(appRepository.appsState.value.none { it.isHidden })
    }
}
