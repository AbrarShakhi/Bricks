package com.abrarshakhi.bricks.core.domain.usecase

import com.abrarshakhi.bricks.core.domain.gateway.AppLauncher
import com.abrarshakhi.bricks.core.domain.gateway.DefaultAppsProvider
import com.abrarshakhi.bricks.core.domain.gateway.SystemActions
import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppShortcut
import com.abrarshakhi.bricks.core.domain.model.AppUsage
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.model.LauncherSettings
import com.abrarshakhi.bricks.core.domain.model.ProfileType
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.model.UserProfile
import com.abrarshakhi.bricks.core.domain.repository.AppRepository
import com.abrarshakhi.bricks.core.domain.repository.HomeLayoutRepository
import com.abrarshakhi.bricks.core.domain.repository.SettingsRepository
import com.abrarshakhi.bricks.core.domain.repository.ShortcutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet

fun testApp(
    name: String,
    serial: Long = 0L,
    hidden: Boolean = false,
    launches: Int = 0,
    installedAt: Long = 0L,
    label: String = name.replaceFirstChar { it.uppercase() },
) = App(
    key = AppKey("pkg.$name", "$name.Main", serial),
    label = label,
    systemLabel = name.replaceFirstChar { it.uppercase() },
    profile = if (serial == 0L) ProfileType.Personal else ProfileType.Work,
    installedAt = installedAt,
    isSystem = false,
    isHidden = hidden,
    isSuspended = false,
    usage = AppUsage(launchCount = launches, lastLaunchedAt = 0L),
)

class FakeAppRepository(
    apps: List<App> = emptyList(),
    profiles: List<UserProfile> = listOf(UserProfile(0L, ProfileType.Personal, isPaused = false)),
) : AppRepository {
    val appsState = MutableStateFlow(apps)
    val profilesState = MutableStateFlow(profiles)
    val launches = mutableListOf<Pair<AppKey, Long>>()
    val labels = mutableMapOf<AppKey, String?>()
    var knownApps: Set<AppKey>? = null

    override val apps: Flow<List<App>> = appsState
    override val profiles: Flow<List<UserProfile>> = profilesState

    override suspend fun setHidden(key: AppKey, hidden: Boolean) {
        appsState.value = appsState.value.map { if (it.key == key) it.copy(isHidden = hidden) else it }
    }

    override suspend fun setCustomLabel(key: AppKey, label: String?) {
        labels[key] = label
    }

    override suspend fun recordLaunch(key: AppKey, timestamp: Long) {
        launches += key to timestamp
    }

    override suspend fun setProfilePaused(serial: Long, paused: Boolean): Boolean = true

    override suspend fun consumeNewlyInstalled(installed: Set<AppKey>): Set<AppKey> {
        val previous = knownApps
        knownApps = installed
        return if (previous == null) emptySet() else installed - previous
    }
}

class FakeHomeLayoutRepository(initial: HomeLayout = HomeLayout.Empty) : HomeLayoutRepository {
    val state = MutableStateFlow(initial)
    override val layout: Flow<HomeLayout> = state

    override suspend fun update(transform: (HomeLayout) -> HomeLayout): HomeLayout = state.updateAndGet(transform)
}

class FakeSettingsRepository(initial: LauncherSettings = LauncherSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<LauncherSettings> = state.map { it }

    override suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        state.value = transform(state.value)
    }
}

class FakeAppLauncher(var result: Boolean = true) : AppLauncher {
    val launched = mutableListOf<AppKey>()
    override fun launch(app: AppKey, bounds: ScreenBounds?): Boolean {
        launched += app
        return result
    }

    override fun openAppInfo(app: AppKey, bounds: ScreenBounds?): Boolean = result
    override fun requestUninstall(app: AppKey): Boolean = result
    override fun openStoreListing(packageName: String): Boolean = result
    override fun searchWeb(query: String): Boolean = result
    override fun searchStore(query: String): Boolean = result
}

class FakeSystemActions(
    override var isLockScreenReady: Boolean = false,
    override var isDefaultLauncher: Boolean = true,
) : SystemActions {
    var locked = false
    override fun expandNotifications(): Boolean = true
    override fun expandQuickSettings(): Boolean = true
    override fun lockScreen(): Boolean {
        locked = true
        return true
    }

    override fun openWallpaperPicker(): Boolean = true
    override fun openLockScreenSetup(): Boolean = true
    override fun openNotificationAccessSettings(): Boolean = true
    override fun openDefaultLauncherSettings(): Boolean = true
    override fun openClock(): Boolean = true
    override fun openCalendar(timeMillis: Long): Boolean = true
    override fun openBatteryUsage(): Boolean = true
}

class FakeShortcutRepository(
    private val shortcuts: List<AppShortcut>,
    override val isAvailable: Boolean = true,
) : ShortcutRepository {
    override suspend fun shortcutsFor(app: AppKey): List<AppShortcut> = shortcuts
    override suspend fun pinnedShortcuts(keys: Set<ShortcutKey>): List<AppShortcut> = emptyList()
    override fun launch(shortcut: ShortcutKey, bounds: ScreenBounds?): Boolean = true
    override suspend fun pin(shortcut: ShortcutKey) = Unit
    override suspend fun unpin(shortcut: ShortcutKey) = Unit
}

class FakeDefaultAppsProvider(
    private val dock: List<AppKey> = emptyList(),
    private val home: List<AppKey> = emptyList(),
) : DefaultAppsProvider {
    override suspend fun dockApps(): List<AppKey> = dock
    override suspend fun homeApps(): List<AppKey> = home
}
