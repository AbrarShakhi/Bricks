package com.abrarshakhi.bricks.core.data.badges

import com.abrarshakhi.bricks.core.domain.model.PackageUserKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationBadgeStore {
    private val badgeState = MutableStateFlow<Map<PackageUserKey, Int>>(emptyMap())
    private val connectedState = MutableStateFlow(false)

    val badges: StateFlow<Map<PackageUserKey, Int>> = badgeState.asStateFlow()
    val connected: StateFlow<Boolean> = connectedState.asStateFlow()

    internal fun publish(badges: Map<PackageUserKey, Int>) {
        badgeState.value = badges
    }

    internal fun setConnected(connected: Boolean) {
        connectedState.value = connected
        if (!connected) badgeState.value = emptyMap()
    }
}
