package com.abrarshakhi.bricks.feature.drawer.di

import com.abrarshakhi.bricks.feature.drawer.DrawerActions
import com.abrarshakhi.bricks.feature.drawer.DrawerObservers
import com.abrarshakhi.bricks.feature.drawer.DrawerViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val drawerModule = module {
    factoryOf(::DrawerObservers)
    factoryOf(::DrawerActions)
    viewModelOf(::DrawerViewModel)
}
