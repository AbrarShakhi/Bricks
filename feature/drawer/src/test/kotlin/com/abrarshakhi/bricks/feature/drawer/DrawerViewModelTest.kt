package com.abrarshakhi.bricks.feature.drawer

import app.cash.turbine.test
import com.abrarshakhi.bricks.core.domain.gateway.Clock
import com.abrarshakhi.bricks.core.domain.model.AppSort
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.search.AppSearchEngine
import com.abrarshakhi.bricks.core.domain.search.ExpressionEvaluator
import com.abrarshakhi.bricks.core.domain.testing.FakeAppLauncher
import com.abrarshakhi.bricks.core.domain.testing.FakeAppRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeHomeLayoutRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeNotificationBadgeRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeSettingsRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeShortcutRepository
import com.abrarshakhi.bricks.core.domain.testing.MainDispatcherRule
import com.abrarshakhi.bricks.core.domain.testing.testApp
import com.abrarshakhi.bricks.core.domain.usecase.apps.EvaluateExpressionUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.LaunchAppUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveProfilesUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveSuggestedAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveVisibleAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.OpenAppInfoUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.OpenStoreListingUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.RenameAppUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.SearchAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.SearchExternallyUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.SetAppHiddenUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.SetProfilePausedUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.UninstallAppUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.AddAppToDockUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.AddAppToHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.PinShortcutToHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ObserveSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.UpdateSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.GetAppShortcutsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.LaunchShortcutUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveNotificationBadgesUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DrawerViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val chrome = testApp("chrome")
    private val calendar = testApp("calendar")
    private val maps = testApp("maps")
    private val zoom = testApp("zoom")
    private val appRepository = FakeAppRepository(apps = listOf(zoom, chrome, maps, calendar))
    private val settingsRepository = FakeSettingsRepository()
    private val layoutRepository = FakeHomeLayoutRepository()
    private val launcher = FakeAppLauncher()
    private val shortcuts = FakeShortcutRepository()
    private val clock = Clock { NOW }

    private fun viewModel() = DrawerViewModel(
        observers = DrawerObservers(
            visibleApps = ObserveVisibleAppsUseCase(appRepository, settingsRepository, clock),
            suggested = ObserveSuggestedAppsUseCase(appRepository, clock),
            profiles = ObserveProfilesUseCase(appRepository),
            settings = ObserveSettingsUseCase(settingsRepository),
            badges = ObserveNotificationBadgesUseCase(FakeNotificationBadgeRepository(), settingsRepository),
        ),
        actions = DrawerActions(
            launch = LaunchAppUseCase(launcher, appRepository, clock),
            search = SearchAppsUseCase(AppSearchEngine(), clock),
            evaluate = EvaluateExpressionUseCase(ExpressionEvaluator()),
            shortcuts = GetAppShortcutsUseCase(shortcuts),
            launchShortcut = LaunchShortcutUseCase(shortcuts),
            pinShortcut = PinShortcutToHomeUseCase(layoutRepository, settingsRepository, shortcuts),
            appInfo = OpenAppInfoUseCase(launcher),
            uninstall = UninstallAppUseCase(launcher),
            setHidden = SetAppHiddenUseCase(appRepository),
            rename = RenameAppUseCase(appRepository),
            addToHome = AddAppToHomeUseCase(layoutRepository, settingsRepository),
            addToDock = AddAppToDockUseCase(layoutRepository, settingsRepository),
            store = OpenStoreListingUseCase(launcher),
            external = SearchExternallyUseCase(launcher),
            pauseProfile = SetProfilePausedUseCase(appRepository),
            updateSettings = UpdateSettingsUseCase(settingsRepository),
        ),
    )

    @Test
    fun `apps are listed alphabetically under letter headers`() {
        val state = viewModel().state.value

        assertTrue(state.ready)
        assertEquals(listOf("C", "M", "Z"), state.sections.map { it.letter })
        assertEquals(
            listOf("Calendar", "Chrome", "Maps", "Zoom"),
            state.rows.filterIsInstance<DrawerRow.Item>().map { it.app.label },
        )
        state.sections.forEach { section ->
            assertEquals(DrawerRow.Header(section.letter), state.rows[section.rowIndex])
        }
    }

    @Test
    fun `sorting by usage drops the letter sections`() {
        val viewModel = viewModel()

        viewModel.onIntent(DrawerIntent.ChangeSort(AppSort.MostUsed))

        assertEquals(AppSort.MostUsed, settingsRepository.state.value.drawer.sort)
        assertTrue(viewModel.state.value.sections.isEmpty())
        assertTrue(viewModel.state.value.rows.none { it is DrawerRow.Header })
    }

    @Test
    fun `typing searches apps and evaluates arithmetic`() {
        val viewModel = viewModel()

        viewModel.onIntent(DrawerIntent.QueryChanged("chr"))
        assertEquals(chrome.key, viewModel.state.value.results.first().key)

        viewModel.onIntent(DrawerIntent.QueryChanged("(2 + 3) * 4"))
        assertEquals("20", viewModel.state.value.calculator?.result)
    }

    @Test
    fun `submitting a search launches the best match`() {
        val viewModel = viewModel()

        viewModel.onIntent(DrawerIntent.QueryChanged("maps"))
        viewModel.onIntent(DrawerIntent.SubmitSearch)

        assertEquals(listOf(maps.key), launcher.launched)
    }

    @Test
    fun `copying a calculation shares the result`() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(DrawerIntent.QueryChanged("6 * 7"))

        viewModel.effects.test {
            viewModel.onIntent(DrawerIntent.SubmitSearch)
            assertEquals(DrawerEffect.CopyText("42"), awaitItem())
            assertEquals(DrawerEffect.ShowMessage(DrawerMessage.Copied), awaitItem())
        }
    }

    @Test
    fun `hiding an app removes it from the drawer`() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onIntent(DrawerIntent.MenuAction(maps.key, DrawerAppAction.Hide))
            assertEquals(DrawerEffect.ShowMessage(DrawerMessage.AppHidden), awaitItem())
        }
        assertTrue(viewModel.state.value.rows.none { it is DrawerRow.Item && it.app.key == maps.key })
        assertEquals(listOf("C", "Z"), viewModel.state.value.sections.map { it.letter })
    }

    @Test
    fun `adding to the dock places the app there`() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onIntent(DrawerIntent.MenuAction(chrome.key, DrawerAppAction.AddToDock))
            assertEquals(DrawerEffect.ShowMessage(DrawerMessage.AddedToDock), awaitItem())
        }
        assertTrue(layoutRepository.state.value.items.any { it.placement is Placement.Dock })
    }

    @Test
    fun `closing the drawer clears the search`() {
        val viewModel = viewModel()
        viewModel.onIntent(DrawerIntent.QueryChanged("cal"))

        viewModel.onIntent(DrawerIntent.Closed)

        assertEquals("", viewModel.state.value.query)
        assertTrue(viewModel.state.value.results.isEmpty())
        assertNull(viewModel.state.value.calculator)
    }

    private companion object {
        const val NOW = 1_000_000L
    }
}
