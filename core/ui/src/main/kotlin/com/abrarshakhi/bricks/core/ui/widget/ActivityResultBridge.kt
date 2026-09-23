package com.abrarshakhi.bricks.core.ui.widget

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class ActivityResultEvent(
    val requestCode: Int,
    val resultCode: Int,
)

class ActivityResultBridge {
    private val events = MutableSharedFlow<ActivityResultEvent>(extraBufferCapacity = 8)

    val results: SharedFlow<ActivityResultEvent> = events.asSharedFlow()

    fun dispatch(requestCode: Int, resultCode: Int) {
        events.tryEmit(ActivityResultEvent(requestCode, resultCode))
    }
}
