package com.abrarshakhi.bricks.di

import com.abrarshakhi.bricks.core.data.di.dataModule
import com.abrarshakhi.bricks.core.ui.di.uiModule
import com.abrarshakhi.bricks.feature.drawer.di.drawerModule
import com.abrarshakhi.bricks.feature.home.di.homeModule
import com.abrarshakhi.bricks.feature.settings.di.settingsModule
import com.abrarshakhi.bricks.feature.widgets.di.widgetsModule
import com.abrarshakhi.bricks.shell.AppShellViewModel
import com.abrarshakhi.bricks.shell.LauncherStartup
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val shellModule = module {
    singleOf(::LauncherStartup)
    viewModelOf(::AppShellViewModel)
}

val appModules: List<Module> = listOf(
    dataModule,
    domainModule,
    uiModule,
    homeModule,
    drawerModule,
    settingsModule,
    widgetsModule,
    shellModule,
)
