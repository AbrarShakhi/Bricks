package com.abrarshakhi.bricks.core.data.device

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.abrarshakhi.bricks.core.data.ApplicationScope
import com.abrarshakhi.bricks.core.domain.model.BatteryStatus
import com.abrarshakhi.bricks.core.domain.repository.DeviceStatusRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn

internal class AndroidDeviceStatusRepository(
    private val context: Context,
    appScope: ApplicationScope,
) : DeviceStatusRepository {

    override val clock: Flow<Long> = broadcasts(
        Intent.ACTION_TIME_TICK,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_DATE_CHANGED,
        Intent.ACTION_SCREEN_ON,
        Intent.ACTION_USER_PRESENT,
    ) { System.currentTimeMillis() }
        .shareIn(appScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    override val battery: Flow<BatteryStatus?> = broadcasts(Intent.ACTION_BATTERY_CHANGED) { intent ->
        intent?.toBatteryStatus()
    }
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    override val nextAlarm: Flow<Long?> = broadcasts(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED) {
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime
    }
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    private fun <T> broadcasts(vararg actions: String, read: (Intent?) -> T): Flow<T> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(read(intent))
            }
        }
        val filter = IntentFilter().apply { actions.forEach(::addAction) }
        val sticky = ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        trySend(read(sticky))
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate()

    private fun Intent.toBatteryStatus(): BatteryStatus? {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        val status = getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        return BatteryStatus(
            level = (level * 100 / scale).coerceIn(0, 100),
            isCharging = plugged != 0 ||
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
