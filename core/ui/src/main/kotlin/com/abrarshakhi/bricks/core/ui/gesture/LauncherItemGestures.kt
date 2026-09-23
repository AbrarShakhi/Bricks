package com.abrarshakhi.bricks.core.ui.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
interface LauncherItemGestureCallbacks {
    fun onPressChanged(pressed: Boolean) = Unit
    fun onTap() = Unit
    fun onLongPress() = Unit
    fun onDragStart(rootPosition: Offset, grabOffset: Offset) = Unit
    fun onDrag(rootPosition: Offset) = Unit
    fun onDragEnd() = Unit
    fun onDragCancel() = Unit
}

@Composable
fun Modifier.launcherItemGestures(
    callbacks: LauncherItemGestureCallbacks,
    enabled: Boolean = true,
    dragEnabled: Boolean = true,
    longPressEnabled: Boolean = true,
): Modifier {
    val latest by rememberUpdatedState(callbacks)
    val haptics = LocalHapticFeedback.current
    val holder = remember { CoordinatesRef() }
    return this
        .onGloballyPositioned { holder.value = it }
        .pointerInput(enabled, dragEnabled, longPressEnabled) {
            if (!enabled) return@pointerInput
            coroutineScope {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pressJob = launch {
                        delay(PRESS_FEEDBACK_DELAY_MILLIS)
                        latest.onPressChanged(true)
                    }
                    val outcome = if (longPressEnabled) {
                        awaitTapOrLongPress(down, viewConfiguration.longPressTimeoutMillis, viewConfiguration.touchSlop)
                    } else {
                        awaitTapOrLongPress(down, Long.MAX_VALUE, viewConfiguration.touchSlop)
                    }
                    pressJob.cancel()
                    when (outcome) {
                        is PressOutcome.Tap -> {
                            latest.onPressChanged(false)
                            outcome.up.consume()
                            latest.onTap()
                        }
                        PressOutcome.Cancelled -> latest.onPressChanged(false)
                        PressOutcome.LongPress -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            latest.onPressChanged(true)
                            latest.onLongPress()
                            trackDrag(
                                down = down,
                                slop = viewConfiguration.touchSlop,
                                dragEnabled = dragEnabled,
                                toRoot = { local -> holder.value?.takeIf { it.isAttached }?.localToRoot(local) ?: local },
                                callbacks = latest,
                            )
                            latest.onPressChanged(false)
                        }
                    }
                }
            }
        }
}

private class CoordinatesRef {
    var value: LayoutCoordinates? = null
}

private sealed interface PressOutcome {
    data class Tap(val up: PointerInputChange) : PressOutcome
    data object Cancelled : PressOutcome
    data object LongPress : PressOutcome
}

private suspend fun AwaitPointerEventScope.awaitTapOrLongPress(
    down: PointerInputChange,
    timeoutMillis: Long,
    slop: Float,
): PressOutcome = try {
    withTimeout(timeoutMillis) {
        var outcome: PressOutcome? = null
        while (outcome == null) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id }
            outcome = when {
                change == null -> PressOutcome.Cancelled
                change.changedToUpIgnoreConsumed() ->
                    if (change.isConsumed) PressOutcome.Cancelled else PressOutcome.Tap(change)
                change.isConsumed -> PressOutcome.Cancelled
                (change.position - down.position).getDistance() > slop -> PressOutcome.Cancelled
                else -> null
            }
        }
        outcome
    }
} catch (_: PointerEventTimeoutCancellationException) {
    PressOutcome.LongPress
}

private suspend fun AwaitPointerEventScope.trackDrag(
    down: PointerInputChange,
    slop: Float,
    dragEnabled: Boolean,
    toRoot: (Offset) -> Offset,
    callbacks: LauncherItemGestureCallbacks,
) {
    var dragging = false
    var finished = false
    try {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (change.changedToUpIgnoreConsumed()) {
                change.consume()
                if (dragging) callbacks.onDragEnd()
                finished = true
                break
            }
            if (!dragging && dragEnabled && (change.position - down.position).getDistance() > slop) {
                dragging = true
                callbacks.onDragStart(toRoot(change.position), down.position)
            }
            if (dragging) callbacks.onDrag(toRoot(change.position))
            change.consume()
        }
    } finally {
        if (dragging && !finished) callbacks.onDragCancel()
    }
}

private const val PRESS_FEEDBACK_DELAY_MILLIS = 70L
