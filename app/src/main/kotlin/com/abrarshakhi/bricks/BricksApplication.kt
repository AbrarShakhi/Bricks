package com.abrarshakhi.bricks

import android.app.Application
import com.abrarshakhi.bricks.di.appModules
import com.abrarshakhi.bricks.shell.LauncherStartup
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BricksApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@BricksApplication)
            modules(appModules)
        }.koin
        koin.get<LauncherStartup>().start()
    }
}
