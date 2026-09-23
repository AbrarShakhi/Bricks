package com.abrarshakhi.bricks.feature.home.di

import com.abrarshakhi.bricks.feature.home.HomeAppActions
import com.abrarshakhi.bricks.feature.home.HomeLayoutActions
import com.abrarshakhi.bricks.feature.home.HomeObservers
import com.abrarshakhi.bricks.feature.home.HomeSystemActions
import com.abrarshakhi.bricks.feature.home.HomeViewModel
import com.abrarshakhi.bricks.feature.home.pin.PinItemViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    factoryOf(::HomeObservers)
    factoryOf(::HomeAppActions)
    factoryOf(::HomeLayoutActions)
    factoryOf(::HomeSystemActions)
    viewModelOf(::HomeViewModel)
    viewModelOf(::PinItemViewModel)
}
