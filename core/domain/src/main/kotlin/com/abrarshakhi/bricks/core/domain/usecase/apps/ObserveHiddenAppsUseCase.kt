package com.abrarshakhi.bricks.core.domain.usecase.apps

import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.repository.AppRepository
import com.abrarshakhi.bricks.core.domain.search.AppSearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveHiddenAppsUseCase(
    private val appRepository: AppRepository,
) {
    operator fun invoke(): Flow<List<App>> = appRepository.apps.map { apps ->
        apps.filter { it.isHidden }.sortedBy { AppSearchEngine.normalize(it.label) }
    }
}
