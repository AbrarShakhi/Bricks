package com.abrarshakhi.bricks.core.domain.usecase.home

import com.abrarshakhi.bricks.core.domain.gateway.DefaultAppsProvider
import com.abrarshakhi.bricks.core.domain.layout.HomeLayoutEditor
import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.GridCell
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.HomeItem
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.model.HomeSettings
import com.abrarshakhi.bricks.core.domain.model.PackageUserKey
import com.abrarshakhi.bricks.core.domain.model.UserProfile
import com.abrarshakhi.bricks.core.domain.repository.AppRepository
import com.abrarshakhi.bricks.core.domain.repository.HomeLayoutRepository
import com.abrarshakhi.bricks.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class SynchronizeHomeLayoutUseCase(
    private val homeRepository: HomeLayoutRepository,
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val defaultApps: DefaultAppsProvider,
) {
    suspend operator fun invoke() {
        combine(appRepository.apps, appRepository.profiles) { apps, profiles -> apps to profiles }
            .collect { (apps, profiles) -> synchronize(apps, profiles) }
    }

    private suspend fun synchronize(apps: List<App>, profiles: List<UserProfile>) {
        if (apps.isEmpty()) return
        val installed = apps.map { it.key }.toSet()
        val newlyInstalled = appRepository.consumeNewlyInstalled(installed)
        val settings = settingsRepository.homeSettings()
        val layout = homeRepository.layout.first()
        val seed = if (!layout.isInitialized) defaultLayout(installed, settings) else null
        homeRepository.update { current ->
            val base = if (!current.isInitialized) seed ?: current.copy(isInitialized = true) else current
            val cleaned = removeUninstalled(base, apps, profiles)
            if (settings.autoAddNewApps && current.isInitialized) {
                addNewApps(cleaned, newlyInstalled, apps, settings)
            } else {
                cleaned
            }
        }
    }

    private suspend fun defaultLayout(installed: Set<AppKey>, settings: HomeSettings): HomeLayout {
        val grid = settings.gridOrDefault()
        var layout = HomeLayout.Empty
        val dock = defaultApps.dockApps().filter { it in installed }.distinct().take(settings.dockSlots)
        dock.forEach { app ->
            layout = HomeLayoutEditor.addToDock(layout, settings.dockSlots, null) { id, placement ->
                HomeItem.Application(id = id, placement = placement, app = app)
            } ?: layout
        }
        defaultApps.homeApps()
            .filter { it in installed && it !in dock }
            .distinct()
            .take(grid.columns)
            .forEach { app ->
                layout = HomeLayoutEditor.addToDesktopAt(
                    layout = layout,
                    grid = grid,
                    span = GridSpan.Single,
                    page = 0,
                    cell = GridCell(
                        x = layout.desktopItems(0).size.coerceAtMost(grid.columns - 1),
                        y = grid.rows - 1,
                    ),
                ) { id, placement -> HomeItem.Application(id = id, placement = placement, app = app) }
            }
        return layout.copy(isInitialized = true)
    }

    private fun removeUninstalled(layout: HomeLayout, apps: List<App>, profiles: List<UserProfile>): HomeLayout {
        val bySerial = apps.groupBy { it.key.userSerial }
        val knownSerials = profiles.map { it.serial }.toSet()
        val installedKeys = apps.map { it.key }.toSet()
        val installedPackages = apps.map { it.key.toPackageUserKey() }.toSet()
        fun profileUnreadable(serial: Long): Boolean = serial in knownSerials && bySerial[serial].isNullOrEmpty()
        return HomeLayoutEditor.removeMissing(
            layout = layout,
            keepApp = { key -> key in installedKeys || profileUnreadable(key.userSerial) },
            keepShortcut = { key ->
                profileUnreadable(key.userSerial) ||
                    PackageUserKey(key.packageName, key.userSerial) in installedPackages
            },
        )
    }

    private fun addNewApps(
        layout: HomeLayout,
        newlyInstalled: Set<AppKey>,
        apps: List<App>,
        settings: HomeSettings,
    ): HomeLayout {
        if (newlyInstalled.isEmpty()) return layout
        val grid = settings.gridOrDefault()
        val visible = apps.filter { !it.isHidden }.associateBy { it.key }
        return newlyInstalled
            .filter { it in visible && !HomeLayoutEditor.containsApp(layout, it) }
            .sortedBy { visible.getValue(it).label.lowercase() }
            .fold(layout) { acc, app ->
                HomeLayoutEditor.addToDesktop(acc, grid, GridSpan.Single, 0) { id, placement ->
                    HomeItem.Application(id = id, placement = placement, app = app)
                }
            }
    }
}
