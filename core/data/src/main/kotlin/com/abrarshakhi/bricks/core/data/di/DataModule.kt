package com.abrarshakhi.bricks.core.data.di

import android.appwidget.AppWidgetManager
import android.content.pm.LauncherApps
import android.os.UserManager
import com.abrarshakhi.bricks.core.data.ApplicationScope
import com.abrarshakhi.bricks.core.data.apps.AndroidAppLauncher
import com.abrarshakhi.bricks.core.data.apps.AndroidAppRepository
import com.abrarshakhi.bricks.core.data.apps.AndroidDefaultAppsProvider
import com.abrarshakhi.bricks.core.data.apps.AppMetadataDto
import com.abrarshakhi.bricks.core.data.apps.LauncherAppsCatalog
import com.abrarshakhi.bricks.core.data.apps.PackageChangeMonitor
import com.abrarshakhi.bricks.core.data.badges.AndroidNotificationBadgeRepository
import com.abrarshakhi.bricks.core.data.badges.NotificationBadgeStore
import com.abrarshakhi.bricks.core.data.device.AndroidDeviceStatusRepository
import com.abrarshakhi.bricks.core.data.home.DataStoreHomeLayoutRepository
import com.abrarshakhi.bricks.core.data.home.HomeLayoutDto
import com.abrarshakhi.bricks.core.data.settings.DataStoreSettingsRepository
import com.abrarshakhi.bricks.core.data.settings.SettingsDto
import com.abrarshakhi.bricks.core.data.shortcuts.LauncherAppsShortcutRepository
import com.abrarshakhi.bricks.core.data.store.jsonDataStore
import com.abrarshakhi.bricks.core.data.system.AccessibilityBridge
import com.abrarshakhi.bricks.core.data.system.AndroidSystemActions
import com.abrarshakhi.bricks.core.data.system.ForegroundActivityTracker
import com.abrarshakhi.bricks.core.data.widgets.AndroidWidgetProviderRepository
import com.abrarshakhi.bricks.core.domain.gateway.AppLauncher
import com.abrarshakhi.bricks.core.domain.gateway.Clock
import com.abrarshakhi.bricks.core.domain.gateway.DefaultAppsProvider
import com.abrarshakhi.bricks.core.domain.gateway.SystemActions
import com.abrarshakhi.bricks.core.domain.repository.AppRepository
import com.abrarshakhi.bricks.core.domain.repository.DeviceStatusRepository
import com.abrarshakhi.bricks.core.domain.repository.HomeLayoutRepository
import com.abrarshakhi.bricks.core.domain.repository.NotificationBadgeRepository
import com.abrarshakhi.bricks.core.domain.repository.SettingsRepository
import com.abrarshakhi.bricks.core.domain.repository.ShortcutRepository
import com.abrarshakhi.bricks.core.domain.repository.WidgetProviderRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { ApplicationScope() }
    single<Clock> { Clock.System }
    single(createdAtStart = true) { ForegroundActivityTracker(androidApplication()) }
    single { NotificationBadgeStore() }
    single { AccessibilityBridge() }

    single { androidContext().getSystemService(LauncherApps::class.java) }
    single { androidContext().getSystemService(UserManager::class.java) }
    single { AppWidgetManager.getInstance(androidContext()) }

    single { PackageChangeMonitor(androidContext(), get(), get<ApplicationScope>()) }
    single { LauncherAppsCatalog(androidContext(), get(), get(), get(), get<ApplicationScope>()) }

    single<SettingsRepository> {
        DataStoreSettingsRepository(
            androidContext().jsonDataStore(SETTINGS_FILE, SettingsDto.serializer(), SettingsDto()),
        )
    }
    single<HomeLayoutRepository> {
        DataStoreHomeLayoutRepository(
            androidContext().jsonDataStore(HOME_FILE, HomeLayoutDto.serializer(), HomeLayoutDto()),
        )
    }
    single<AppRepository> {
        AndroidAppRepository(
            catalog = get(),
            metadata = androidContext().jsonDataStore(METADATA_FILE, AppMetadataDto.serializer(), AppMetadataDto()),
            userManager = get(),
            appScope = get<ApplicationScope>(),
        )
    }
    single<AppLauncher> { AndroidAppLauncher(androidContext(), get(), get(), get()) }
    single<DefaultAppsProvider> { AndroidDefaultAppsProvider(androidContext(), get(), get()) }
    single<ShortcutRepository> { LauncherAppsShortcutRepository(get(), get()) }
    single<WidgetProviderRepository> {
        AndroidWidgetProviderRepository(androidContext(), get(), get(), get(), get(), get<ApplicationScope>())
    }
    single<DeviceStatusRepository> { AndroidDeviceStatusRepository(androidContext(), get<ApplicationScope>()) }
    single<NotificationBadgeRepository> { AndroidNotificationBadgeRepository(get()) }
    single<SystemActions> { AndroidSystemActions(androidContext(), get(), get()) }
}

private const val SETTINGS_FILE = "launcher_settings.json"
private const val HOME_FILE = "home_layout.json"
private const val METADATA_FILE = "app_metadata.json"
