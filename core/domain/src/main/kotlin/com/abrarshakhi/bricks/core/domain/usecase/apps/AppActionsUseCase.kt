package com.abrarshakhi.bricks.core.domain.usecase.apps

import com.abrarshakhi.bricks.core.domain.gateway.AppLauncher
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds

class OpenAppInfoUseCase(
    private val appLauncher: AppLauncher,
) {
    operator fun invoke(app: AppKey, bounds: ScreenBounds?): Boolean = appLauncher.openAppInfo(app, bounds)
}

class UninstallAppUseCase(
    private val appLauncher: AppLauncher,
) {
    operator fun invoke(app: AppKey): Boolean = appLauncher.requestUninstall(app)
}

class OpenStoreListingUseCase(
    private val appLauncher: AppLauncher,
) {
    operator fun invoke(packageName: String): Boolean = appLauncher.openStoreListing(packageName)
}

class SearchExternallyUseCase(
    private val appLauncher: AppLauncher,
) {
    fun web(query: String): Boolean = query.isNotBlank() && appLauncher.searchWeb(query.trim())

    fun store(query: String): Boolean = query.isNotBlank() && appLauncher.searchStore(query.trim())
}
