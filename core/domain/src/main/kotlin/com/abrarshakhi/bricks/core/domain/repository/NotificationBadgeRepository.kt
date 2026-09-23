package com.abrarshakhi.bricks.core.domain.repository

import com.abrarshakhi.bricks.core.domain.model.PackageUserKey
import kotlinx.coroutines.flow.Flow

interface NotificationBadgeRepository {
    val badges: Flow<Map<PackageUserKey, Int>>
    val isAccessGranted: Flow<Boolean>
}
