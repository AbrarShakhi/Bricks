package com.abrarshakhi.bricks.feature.settings.di

import com.abrarshakhi.bricks.feature.settings.HiddenAppsViewModel
import com.abrarshakhi.bricks.feature.settings.SettingsUseCases
import com.abrarshakhi.bricks.feature.settings.SettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    factoryOf(::SettingsUseCases)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::HiddenAppsViewModel)
}
