package com.abrarshakhi.bricks.core.domain.gateway

import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds

interface AppLauncher {
    fun launch(app: AppKey, bounds: ScreenBounds?): Boolean

    fun openAppInfo(app: AppKey, bounds: ScreenBounds?): Boolean

    fun requestUninstall(app: AppKey): Boolean

    fun openStoreListing(packageName: String): Boolean

    fun searchWeb(query: String): Boolean

    fun searchStore(query: String): Boolean
}
