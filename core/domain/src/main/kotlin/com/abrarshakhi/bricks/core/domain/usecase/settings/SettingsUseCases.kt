package com.abrarshakhi.bricks.core.domain.usecase.settings

import com.abrarshakhi.bricks.core.domain.layout.HomeLayoutEditor
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.HomeSettings
import com.abrarshakhi.bricks.core.domain.model.LauncherSettings
import com.abrarshakhi.bricks.core.domain.repository.HomeLayoutRepository
import com.abrarshakhi.bricks.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ObserveSettingsUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<LauncherSettings> = repository.settings
}

class UpdateSettingsUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(transform: (LauncherSettings) -> LauncherSettings) = repository.update(transform)
}

class EnsureHomeGridUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(smallestWidthDp: Int, largestWidthDp: Int): GridSize {
        val current = repository.settings.first().home.grid
        if (current != null) return current
        val recommended = GridSize.recommendedFor(smallestWidthDp, largestWidthDp)
        repository.update { settings ->
            if (settings.home.grid != null) {
                settings
            } else {
                settings.copy(
                    home = settings.home.copy(
                        grid = recommended,
                        dockSlots = recommended.columns.coerceIn(HomeSettings.MIN_DOCK_SLOTS, HomeSettings.MAX_DOCK_SLOTS),
                    ),
                )
            }
        }
        return repository.settings.first().home.grid ?: recommended
    }
}

class ChangeHomeGridUseCase(
    private val settingsRepository: SettingsRepository,
    private val homeRepository: HomeLayoutRepository,
) {
    suspend operator fun invoke(columns: Int, rows: Int) {
        val grid = GridSize.of(columns, rows)
        settingsRepository.update { it.copy(home = it.home.copy(grid = grid)) }
        val dockSlots = settingsRepository.settings.first().home.dockSlots
        homeRepository.update { layout -> HomeLayoutEditor.reflow(layout, grid, dockSlots) }
    }
}

class ChangeDockSlotsUseCase(
    private val settingsRepository: SettingsRepository,
    private val homeRepository: HomeLayoutRepository,
) {
    suspend operator fun invoke(slots: Int) {
        val clamped = slots.coerceIn(HomeSettings.MIN_DOCK_SLOTS, HomeSettings.MAX_DOCK_SLOTS)
        settingsRepository.update { it.copy(home = it.home.copy(dockSlots = clamped)) }
        val grid = settingsRepository.settings.first().home.grid ?: GridSize.of(4, 5)
        homeRepository.update { layout -> HomeLayoutEditor.reflow(layout, grid, clamped) }
    }
}
