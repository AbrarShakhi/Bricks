package com.abrarshakhi.bricks.core.domain.repository

import com.abrarshakhi.bricks.core.domain.model.LauncherSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<LauncherSettings>

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings)
}
