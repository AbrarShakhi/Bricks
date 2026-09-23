package com.abrarshakhi.bricks.core.domain.repository

import com.abrarshakhi.bricks.core.domain.model.BatteryStatus
import kotlinx.coroutines.flow.Flow

interface DeviceStatusRepository {
    val clock: Flow<Long>
    val battery: Flow<BatteryStatus?>
    val nextAlarm: Flow<Long?>
}
