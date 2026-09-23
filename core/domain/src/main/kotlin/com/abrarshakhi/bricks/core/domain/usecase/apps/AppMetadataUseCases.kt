package com.abrarshakhi.bricks.core.domain.usecase.apps

import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.UserProfile
import com.abrarshakhi.bricks.core.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow

class SetAppHiddenUseCase(
    private val appRepository: AppRepository,
) {
    suspend operator fun invoke(app: AppKey, hidden: Boolean) = appRepository.setHidden(app, hidden)
}

class RenameAppUseCase(
    private val appRepository: AppRepository,
) {
    suspend operator fun invoke(app: AppKey, label: String?, systemLabel: String) {
        val trimmed = label?.trim()?.take(MAX_LABEL_LENGTH)
        val custom = trimmed?.takeIf { it.isNotEmpty() && it != systemLabel }
        appRepository.setCustomLabel(app, custom)
    }

    private companion object {
        const val MAX_LABEL_LENGTH = 40
    }
}

class ObserveProfilesUseCase(
    private val appRepository: AppRepository,
) {
    operator fun invoke(): Flow<List<UserProfile>> = appRepository.profiles
}

class SetProfilePausedUseCase(
    private val appRepository: AppRepository,
) {
    suspend operator fun invoke(serial: Long, paused: Boolean): Boolean =
        appRepository.setProfilePaused(serial, paused)
}
