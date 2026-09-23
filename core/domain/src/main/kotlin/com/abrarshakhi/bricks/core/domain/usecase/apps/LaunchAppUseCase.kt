package com.abrarshakhi.bricks.core.domain.usecase.apps

import com.abrarshakhi.bricks.core.domain.gateway.AppLauncher
import com.abrarshakhi.bricks.core.domain.gateway.Clock
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds
import com.abrarshakhi.bricks.core.domain.repository.AppRepository

class LaunchAppUseCase(
    private val appLauncher: AppLauncher,
    private val appRepository: AppRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(app: AppKey, bounds: ScreenBounds?): Boolean {
        val launched = appLauncher.launch(app, bounds)
        if (launched) appRepository.recordLaunch(app, clock.now())
        return launched
    }
}
