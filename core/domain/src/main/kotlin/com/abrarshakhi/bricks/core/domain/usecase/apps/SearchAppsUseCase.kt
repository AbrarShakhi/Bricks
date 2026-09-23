package com.abrarshakhi.bricks.core.domain.usecase.apps

import com.abrarshakhi.bricks.core.domain.gateway.Clock
import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.search.AppSearchEngine

class SearchAppsUseCase(
    private val engine: AppSearchEngine,
    private val clock: Clock,
) {
    operator fun invoke(apps: List<App>, query: String): List<App> =
        engine.search(apps.filterNot { it.isHidden }, query, clock.now())
}
