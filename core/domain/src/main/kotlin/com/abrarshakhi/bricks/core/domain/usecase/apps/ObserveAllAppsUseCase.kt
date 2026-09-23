package com.abrarshakhi.bricks.core.domain.usecase.apps

import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow

class ObserveAllAppsUseCase(
    private val appRepository: AppRepository,
) {
    operator fun invoke(): Flow<List<App>> = appRepository.apps
}
