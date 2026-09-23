package com.abrarshakhi.bricks.core.ui.di

import com.abrarshakhi.bricks.core.ui.icon.AppIconLoader
import com.abrarshakhi.bricks.core.ui.util.HomeButtonEvents
import com.abrarshakhi.bricks.core.ui.widget.ActivityResultBridge
import com.abrarshakhi.bricks.core.ui.widget.LauncherWidgetHost
import com.abrarshakhi.bricks.core.ui.widget.WidgetPreviewLoader
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val uiModule = module {
    single { AppIconLoader(androidContext(), get(), get()) }
    single { LauncherWidgetHost(androidContext(), get()) }
    single { WidgetPreviewLoader(androidContext(), get()) }
    single { ActivityResultBridge() }
    single { HomeButtonEvents() }
}
