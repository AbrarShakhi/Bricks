package com.abrarshakhi.bricks.core.domain.usecase.system

import com.abrarshakhi.bricks.core.domain.gateway.SystemActions
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppShortcut
import com.abrarshakhi.bricks.core.domain.model.GestureAction
import com.abrarshakhi.bricks.core.domain.model.Glance
import com.abrarshakhi.bricks.core.domain.model.PackageUserKey
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.model.WidgetProvider
import com.abrarshakhi.bricks.core.domain.repository.DeviceStatusRepository
import com.abrarshakhi.bricks.core.domain.repository.NotificationBadgeRepository
import com.abrarshakhi.bricks.core.domain.repository.SettingsRepository
import com.abrarshakhi.bricks.core.domain.repository.ShortcutRepository
import com.abrarshakhi.bricks.core.domain.repository.WidgetProviderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

sealed interface GestureOutcome {
    data object Handled : GestureOutcome
    data object Ignored : GestureOutcome
    data object OpenDrawer : GestureOutcome
    data object OpenSearch : GestureOutcome
    data object OpenSettings : GestureOutcome
    data object LockScreenNeedsSetup : GestureOutcome
    data object Failed : GestureOutcome
}

class PerformGestureUseCase(
    private val systemActions: SystemActions,
) {
    operator fun invoke(action: GestureAction): GestureOutcome = when (action) {
        GestureAction.None -> GestureOutcome.Ignored
        GestureAction.OpenDrawer -> GestureOutcome.OpenDrawer
        GestureAction.OpenSearch -> GestureOutcome.OpenSearch
        GestureAction.OpenSettings -> GestureOutcome.OpenSettings
        GestureAction.ExpandNotifications ->
            if (systemActions.expandNotifications()) GestureOutcome.Handled else GestureOutcome.Failed
        GestureAction.ExpandQuickSettings ->
            if (systemActions.expandQuickSettings()) GestureOutcome.Handled else GestureOutcome.Failed
        GestureAction.LockScreen -> when {
            !systemActions.isLockScreenReady -> GestureOutcome.LockScreenNeedsSetup
            systemActions.lockScreen() -> GestureOutcome.Handled
            else -> GestureOutcome.Failed
        }
    }
}

class ObserveGlanceUseCase(
    private val repository: DeviceStatusRepository,
) {
    operator fun invoke(): Flow<Glance> = combine(
        repository.clock,
        repository.battery,
        repository.nextAlarm,
    ) { time, battery, alarm -> Glance(timeMillis = time, battery = battery, nextAlarmMillis = alarm) }
}

class ObserveNotificationBadgesUseCase(
    private val repository: NotificationBadgeRepository,
    private val settingsRepository: SettingsRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Map<PackageUserKey, Int>> = settingsRepository.settings
        .map { it.home.notificationDots }
        .distinctUntilChanged()
        .flatMapLatest { enabled -> if (enabled) repository.badges else flowOf(emptyMap()) }
}

class ObserveNotificationAccessUseCase(
    private val repository: NotificationBadgeRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.isAccessGranted
}

class GetAppShortcutsUseCase(
    private val repository: ShortcutRepository,
) {
    suspend operator fun invoke(app: AppKey): List<AppShortcut> {
        if (!repository.isAvailable) return emptyList()
        return repository.shortcutsFor(app)
            .filter { it.isEnabled }
            .sortedWith(compareBy<AppShortcut> { it.isDynamic }.thenBy { it.rank })
            .take(MAX_SHORTCUTS)
    }

    private companion object {
        const val MAX_SHORTCUTS = 5
    }
}

class LaunchShortcutUseCase(
    private val repository: ShortcutRepository,
) {
    operator fun invoke(shortcut: ShortcutKey, bounds: ScreenBounds?): Boolean = repository.launch(shortcut, bounds)
}

data class WidgetProviderGroup(
    val packageName: String,
    val appLabel: String,
    val providers: List<WidgetProvider>,
)

class ObserveWidgetProvidersUseCase(
    private val repository: WidgetProviderRepository,
) {
    operator fun invoke(): Flow<List<WidgetProviderGroup>> = repository.providers.map { providers ->
        providers
            .groupBy { it.key.packageName to it.key.userSerial }
            .map { (key, group) ->
                WidgetProviderGroup(
                    packageName = key.first,
                    appLabel = group.first().appLabel,
                    providers = group.sortedBy { it.label.lowercase() },
                )
            }
            .sortedBy { it.appLabel.lowercase() }
    }
}

class SystemStatusUseCase(
    private val systemActions: SystemActions,
) {
    val isDefaultLauncher: Boolean
        get() = systemActions.isDefaultLauncher

    val isLockScreenReady: Boolean
        get() = systemActions.isLockScreenReady

    fun openWallpaperPicker(): Boolean = systemActions.openWallpaperPicker()

    fun openLockScreenSetup(): Boolean = systemActions.openLockScreenSetup()

    fun openNotificationAccessSettings(): Boolean = systemActions.openNotificationAccessSettings()

    fun openDefaultLauncherSettings(): Boolean = systemActions.openDefaultLauncherSettings()

    fun openClock(): Boolean = systemActions.openClock()

    fun openCalendar(timeMillis: Long): Boolean = systemActions.openCalendar(timeMillis)

    fun openBatteryUsage(): Boolean = systemActions.openBatteryUsage()
}
