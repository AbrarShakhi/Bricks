package com.abrarshakhi.bricks.core.data.badges

import com.abrarshakhi.bricks.core.domain.model.PackageUserKey
import com.abrarshakhi.bricks.core.domain.repository.NotificationBadgeRepository
import kotlinx.coroutines.flow.Flow

internal class AndroidNotificationBadgeRepository(
    store: NotificationBadgeStore,
) : NotificationBadgeRepository {
    override val badges: Flow<Map<PackageUserKey, Int>> = store.badges
    override val isAccessGranted: Flow<Boolean> = store.connected
}
