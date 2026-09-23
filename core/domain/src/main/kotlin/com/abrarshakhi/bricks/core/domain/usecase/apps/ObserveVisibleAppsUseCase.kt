package com.abrarshakhi.bricks.core.domain.usecase.apps

import com.abrarshakhi.bricks.core.domain.gateway.Clock
import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.model.AppSort
import com.abrarshakhi.bricks.core.domain.repository.AppRepository
import com.abrarshakhi.bricks.core.domain.repository.SettingsRepository
import com.abrarshakhi.bricks.core.domain.search.AppSearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveVisibleAppsUseCase(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<App>> = combine(
        appRepository.apps,
        settingsRepository.settings.map { it.drawer.sort }.distinctUntilChanged(),
    ) { apps, sort ->
        sort(apps.filterNot { it.isHidden }, sort, clock.now())
    }

    companion object {
        fun sort(apps: List<App>, sort: AppSort, now: Long): List<App> {
            val alphabetical = compareBy<App> { AppSearchEngine.normalize(it.label) }.thenBy { it.key.flattened }
            return when (sort) {
                AppSort.Alphabetical -> apps.sortedWith(alphabetical)
                AppSort.MostUsed -> apps.sortedWith(compareByDescending<App> { it.usage.score(now) }.then(alphabetical))
                AppSort.RecentlyInstalled -> apps.sortedWith(compareByDescending<App> { it.installedAt }.then(alphabetical))
            }
        }
    }
}
