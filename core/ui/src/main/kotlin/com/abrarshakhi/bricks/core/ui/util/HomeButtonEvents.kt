package com.abrarshakhi.bricks.core.ui.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class HomeButtonEvents {
    private val events = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)

    val presses: SharedFlow<Boolean> = events.asSharedFlow()

    fun onHomePressed(alreadyVisible: Boolean) {
        events.tryEmit(alreadyVisible)
    }
}
