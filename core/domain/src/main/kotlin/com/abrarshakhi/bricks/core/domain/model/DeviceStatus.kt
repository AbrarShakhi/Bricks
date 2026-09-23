package com.abrarshakhi.bricks.core.domain.model

data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean,
)

data class Glance(
    val timeMillis: Long,
    val battery: BatteryStatus?,
    val nextAlarmMillis: Long?,
)

data class ScreenBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)
