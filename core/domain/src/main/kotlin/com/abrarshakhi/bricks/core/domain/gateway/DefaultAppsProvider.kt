package com.abrarshakhi.bricks.core.domain.gateway

import com.abrarshakhi.bricks.core.domain.model.AppKey

interface DefaultAppsProvider {
    suspend fun dockApps(): List<AppKey>

    suspend fun homeApps(): List<AppKey>
}
