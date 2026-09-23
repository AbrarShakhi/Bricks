package com.abrarshakhi.bricks.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrarshakhi.bricks.core.domain.model.AppearanceSettings
import com.abrarshakhi.bricks.core.domain.usecase.settings.ObserveSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AppShellViewModel(
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {
    val appearance: StateFlow<AppearanceSettings> = observeSettings()
        .map { it.appearance }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())
}
