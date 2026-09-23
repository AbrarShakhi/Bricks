package com.abrarshakhi.bricks.core.domain.repository

import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    val apps: Flow<List<App>>
    val profiles: Flow<List<UserProfile>>

    suspend fun setHidden(key: AppKey, hidden: Boolean)

    suspend fun setCustomLabel(key: AppKey, label: String?)

    suspend fun recordLaunch(key: AppKey, timestamp: Long)

    suspend fun setProfilePaused(serial: Long, paused: Boolean): Boolean

    suspend fun consumeNewlyInstalled(installed: Set<AppKey>): Set<AppKey>
}
