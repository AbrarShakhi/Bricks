package com.abrarshakhi.bricks.feature.widgets.di

import com.abrarshakhi.bricks.feature.widgets.WidgetsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val widgetsModule = module {
    viewModelOf(::WidgetsViewModel)
}
