package com.abrarshakhi.bricks.core.domain.usecase

import app.cash.turbine.test
import com.abrarshakhi.bricks.core.domain.gateway.Clock
import com.abrarshakhi.bricks.core.domain.model.AppShortcut
import com.abrarshakhi.bricks.core.domain.model.AppSort
import com.abrarshakhi.bricks.core.domain.model.DrawerSettings
import com.abrarshakhi.bricks.core.domain.model.GestureAction
import com.abrarshakhi.bricks.core.domain.model.GridCell
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.HomeItem
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.model.HomeSettings
import com.abrarshakhi.bricks.core.domain.model.LauncherSettings
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.usecase.apps.LaunchAppUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveVisibleAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.RenameAppUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.DropAppOnHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.DropTarget
import com.abrarshakhi.bricks.core.domain.usecase.home.RemoveHomeItemUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.SynchronizeHomeLayoutUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ChangeHomeGridUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.EnsureHomeGridUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.GestureOutcome
import com.abrarshakhi.bricks.core.domain.usecase.system.GetAppShortcutsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.PerformGestureUseCase
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCaseTest {

    private val clock = Clock { 42L }
    private val settings = LauncherSettings(home = HomeSettings(grid = GridSize(4, 5), dockSlots = 4))

    @Test
    fun `launching records usage only on success`() = runTest {
        val apps = FakeAppRepository()
        val launcher = FakeAppLauncher(result = true)
        val useCase = LaunchAppUseCase(launcher, apps, clock)
        assertTrue(useCase(testApp("a").key, null))
        launcher.result = false
        assertFalse(useCase(testApp("b").key, null))
        assertEquals(listOf(testApp("a").key to 42L), apps.launches)
    }

    @Test
    fun `renaming to the system label clears the custom label`() = runTest {
        val apps = FakeAppRepository()
        val rename = RenameAppUseCase(apps)
        rename(testApp("a").key, "  Mail  ", "Gmail")
        assertEquals("Mail", apps.labels[testApp("a").key])
        rename(testApp("a").key, "Gmail", "Gmail")
        assertNull(apps.labels[testApp("a").key])
        rename(testApp("a").key, "   ", "Gmail")
        assertNull(apps.labels[testApp("a").key])
    }

    @Test
    fun `visible apps exclude hidden ones and follow the sort setting`() = runTest {
        val apps = FakeAppRepository(
            listOf(
                testApp("banana", installedAt = 1),
                testApp("apple", installedAt = 3),
                testApp("cherry", hidden = true),
                testApp("date", installedAt = 2, launches = 10),
            ),
        )
        val settingsRepository = FakeSettingsRepository(settings)
        val useCase = ObserveVisibleAppsUseCase(apps, settingsRepository, clock)
        useCase().test {
            assertEquals(listOf("Apple", "Banana", "Date"), awaitItem().map { it.label })
            settingsRepository.state.value = settings.copy(drawer = DrawerSettings(sort = AppSort.RecentlyInstalled))
            assertEquals(listOf("Apple", "Date", "Banana"), awaitItem().map { it.label })
            settingsRepository.state.value = settings.copy(drawer = DrawerSettings(sort = AppSort.MostUsed))
            assertEquals("Date", awaitItem().first().label)
        }
    }

    @Test
    fun `first sync seeds the default layout`() = runTest {
        val apps = FakeAppRepository(listOf(testApp("phone"), testApp("camera"), testApp("clock"), testApp("maps")))
        val home = FakeHomeLayoutRepository(HomeLayout.Uninitialized)
        val sync = SynchronizeHomeLayoutUseCase(
            homeRepository = home,
            appRepository = apps,
            settingsRepository = FakeSettingsRepository(settings),
            defaultApps = FakeDefaultAppsProvider(
                dock = listOf(testApp("phone").key, testApp("camera").key, testApp("missing").key),
                home = listOf(testApp("clock").key, testApp("phone").key),
            ),
        )
        val job = launch { sync() }
        runCurrent()
        val layout = home.state.value
        assertTrue(layout.isInitialized)
        assertEquals(
            listOf(testApp("phone").key, testApp("camera").key),
            layout.dockItems.map { (it as HomeItem.Application).app },
        )
        assertEquals(
            listOf(testApp("clock").key),
            layout.desktopItems(0).map { (it as HomeItem.Application).app },
        )
        assertEquals(4, (layout.desktopItems(0).single().placement as Placement.Desktop).cell.y)
        job.cancel()
    }

    @Test
    fun `sync removes uninstalled apps and adds new installs`() = runTest {
        val initial = HomeLayout(
            pageCount = 1,
            items = listOf(
                HomeItem.Application(1, Placement.Desktop(0, GridCell(0, 0)), testApp("old").key),
                HomeItem.Application(2, Placement.Desktop(0, GridCell(1, 0)), testApp("kept").key),
            ),
        )
        val apps = FakeAppRepository(listOf(testApp("old"), testApp("kept")))
        val home = FakeHomeLayoutRepository(initial)
        val sync = SynchronizeHomeLayoutUseCase(home, apps, FakeSettingsRepository(settings), FakeDefaultAppsProvider())
        val job = launch { sync() }
        runCurrent()
        apps.appsState.value = listOf(testApp("kept"), testApp("fresh"))
        runCurrent()
        val installed = home.state.value.items.map { (it as HomeItem.Application).app }
        assertEquals(listOf(testApp("kept").key, testApp("fresh").key), installed)
        job.cancel()
    }

    @Test
    fun `sync keeps items of a profile that cannot be read`() = runTest {
        val workApp = testApp("mail", serial = 10L)
        val initial = HomeLayout(
            pageCount = 1,
            items = listOf(HomeItem.Application(1, Placement.Desktop(0, GridCell(0, 0)), workApp.key)),
        )
        val apps = FakeAppRepository(
            apps = listOf(testApp("kept")),
            profiles = listOf(
                com.abrarshakhi.bricks.core.domain.model.UserProfile(0L, com.abrarshakhi.bricks.core.domain.model.ProfileType.Personal, false),
                com.abrarshakhi.bricks.core.domain.model.UserProfile(10L, com.abrarshakhi.bricks.core.domain.model.ProfileType.Work, true),
            ),
        )
        val home = FakeHomeLayoutRepository(initial)
        val sync = SynchronizeHomeLayoutUseCase(home, apps, FakeSettingsRepository(settings), FakeDefaultAppsProvider())
        val job = launch { sync() }
        runCurrent()
        assertTrue(home.state.value.items.any { it is HomeItem.Application && it.app == workApp.key })
        job.cancel()
    }

    @Test
    fun `dropping a drawer app onto another app creates a folder`() = runTest {
        val initial = HomeLayout(
            pageCount = 1,
            items = listOf(HomeItem.Application(1, Placement.Desktop(0, GridCell(2, 2)), testApp("a").key)),
        )
        val home = FakeHomeLayoutRepository(initial)
        DropAppOnHomeUseCase(home, FakeSettingsRepository(settings))(testApp("b").key, DropTarget.Folder(1, "Folder"))
        val folder = home.state.value.items.filterIsInstance<HomeItem.Folder>().single()
        assertEquals(Placement.Desktop(0, GridCell(2, 2)), folder.placement)
        assertEquals(2, home.state.value.folderContents(folder.id).size)
    }

    @Test
    fun `removing a widget reports its id for release`() = runTest {
        val initial = HomeLayout(
            pageCount = 1,
            items = listOf(HomeItem.Widget(5, Placement.Desktop(0, GridCell(0, 0)), 77, WidgetProviderKey("p", "c", 0))),
        )
        val home = FakeHomeLayoutRepository(initial)
        assertEquals(setOf(77), RemoveHomeItemUseCase(home, FakeShortcutRepository(emptyList()))(5))
        assertTrue(home.state.value.items.isEmpty())
    }

    @Test
    fun `grid is chosen once from the device size`() = runTest {
        val repository = FakeSettingsRepository()
        val ensure = EnsureHomeGridUseCase(repository)
        assertEquals(GridSize(5, 6), ensure(412, 915))
        assertEquals(5, repository.state.value.home.dockSlots)
        assertEquals(GridSize(5, 6), ensure(800, 1280))
    }

    @Test
    fun `changing the grid reflows the layout`() = runTest {
        val initial = HomeLayout(
            pageCount = 1,
            items = listOf(HomeItem.Application(1, Placement.Desktop(0, GridCell(3, 4)), testApp("a").key)),
        )
        val home = FakeHomeLayoutRepository(initial)
        ChangeHomeGridUseCase(FakeSettingsRepository(settings), home)(3, 3)
        val placement = home.state.value.items.single().placement as Placement.Desktop
        assertTrue(GridSize(3, 3).contains(placement.cell))
    }

    @Test
    fun `lock screen gesture asks for setup when unavailable`() {
        val actions = FakeSystemActions(isLockScreenReady = false)
        val perform = PerformGestureUseCase(actions)
        assertEquals(GestureOutcome.LockScreenNeedsSetup, perform(GestureAction.LockScreen))
        actions.isLockScreenReady = true
        assertEquals(GestureOutcome.Handled, perform(GestureAction.LockScreen))
        assertTrue(actions.locked)
        assertEquals(GestureOutcome.OpenDrawer, perform(GestureAction.OpenDrawer))
        assertEquals(GestureOutcome.Ignored, perform(GestureAction.None))
    }

    @Test
    fun `shortcuts list manifest entries first and cap the count`() = runTest {
        fun shortcut(id: String, rank: Int, dynamic: Boolean, enabled: Boolean = true) = AppShortcut(
            key = ShortcutKey("p", id, 0L),
            shortLabel = id,
            longLabel = null,
            rank = rank,
            isDynamic = dynamic,
            isPinned = false,
            isEnabled = enabled,
        )
        val repository = FakeShortcutRepository(
            listOf(
                shortcut("d1", 0, true),
                shortcut("s2", 1, false),
                shortcut("s1", 0, false),
                shortcut("off", 0, false, enabled = false),
                shortcut("d2", 1, true),
                shortcut("d3", 2, true),
                shortcut("d4", 3, true),
            ),
        )
        val result = GetAppShortcutsUseCase(repository)(testApp("a").key).map { it.shortLabel }
        assertEquals(listOf("s1", "s2", "d1", "d2", "d3"), result)
        assertTrue(GetAppShortcutsUseCase(FakeShortcutRepository(emptyList(), isAvailable = false))(testApp("a").key).isEmpty())
    }
}
