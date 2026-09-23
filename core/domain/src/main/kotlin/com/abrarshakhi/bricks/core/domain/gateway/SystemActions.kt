package com.abrarshakhi.bricks.core.domain.gateway

interface SystemActions {
    val isLockScreenReady: Boolean
    val isDefaultLauncher: Boolean

    fun expandNotifications(): Boolean

    fun expandQuickSettings(): Boolean

    fun lockScreen(): Boolean

    fun openWallpaperPicker(): Boolean

    fun openLockScreenSetup(): Boolean

    fun openNotificationAccessSettings(): Boolean

    fun openDefaultLauncherSettings(): Boolean

    fun openClock(): Boolean

    fun openCalendar(timeMillis: Long): Boolean

    fun openBatteryUsage(): Boolean
}
