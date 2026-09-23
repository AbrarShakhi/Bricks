package com.abrarshakhi.bricks.core.domain.usecase.apps

import com.abrarshakhi.bricks.core.domain.gateway.Clock
import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveSuggestedAppsUseCase(
    private val appRepository: AppRepository,
    private val clock: Clock,
) {
    operator fun invoke(limit: Int): Flow<List<App>> = appRepository.apps.map { apps ->
        suggest(apps, limit, clock.now())
    }

    companion object {
        fun suggest(apps: List<App>, limit: Int, now: Long): List<App> = apps
            .asSequence()
            .filter { !it.isHidden && it.usage.launchCount > 0 }
            .sortedByDescending { it.usage.score(now) }
            .take(limit.coerceAtLeast(0))
            .toList()
    }
}
