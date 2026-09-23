package com.abrarshakhi.bricks.core.ui.drawer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs

@Stable
class LauncherDrawerState : DraggableState {

    var progress: Float by mutableFloatStateOf(0f)
        private set

    var heightPx: Float by mutableFloatStateOf(0f)

    var searchRequests: Int by mutableIntStateOf(0)
        private set

    var openRequests: Int by mutableIntStateOf(0)
        private set

    val isVisible: Boolean
        get() = progress > 0f

    val isOpen: Boolean
        get() = progress >= OPEN_THRESHOLD

    private val mutex = MutatorMutex()

    private val dragScope = object : DragScope {
        override fun dragBy(pixels: Float) {
            dispatchRawDelta(pixels)
        }
    }

    override fun dispatchRawDelta(delta: Float) {
        consume(delta)
    }

    override suspend fun drag(dragPriority: MutatePriority, block: suspend DragScope.() -> Unit) = coroutineScope {
        mutex.mutateWith(dragScope, dragPriority, block)
    }

    fun consume(delta: Float): Float {
        if (heightPx <= 0f) return 0f
        val before = progress
        progress = (before - delta / heightPx).coerceIn(0f, 1f)
        return (before - progress) * heightPx
    }

    fun requestOpen(focusSearch: Boolean) {
        if (focusSearch) searchRequests++
        openRequests++
    }

    suspend fun interrupt() {
        mutex.mutate(MutatePriority.UserInput) { }
    }

    suspend fun open(velocity: Float = 0f) = animateTo(1f, velocity)

    suspend fun close(velocity: Float = 0f) = animateTo(0f, velocity)

    suspend fun snapClosed() {
        mutex.mutate(MutatePriority.PreventUserInput) { progress = 0f }
    }

    suspend fun settle(velocity: Float) {
        val target = when {
            velocity < -FLING_VELOCITY -> 1f
            velocity > FLING_VELOCITY -> 0f
            progress > SETTLE_THRESHOLD -> 1f
            else -> 0f
        }
        animateTo(target, velocity)
    }

    private suspend fun animateTo(target: Float, velocity: Float) {
        mutex.mutate {
            val fractionVelocity = if (heightPx > 0f) -velocity / heightPx else 0f
            animate(
                initialValue = progress,
                targetValue = target,
                initialVelocity = fractionVelocity,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
            ) { value, _ -> progress = value.coerceIn(0f, 1f) }
        }
    }

    fun nestedScrollConnection(onSettle: suspend (Float) -> Unit): NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y >= 0f || progress >= 1f) return Offset.Zero
            return Offset(0f, consume(available.y))
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            return Offset(0f, consume(available.y))
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (progress >= 1f || progress <= 0f) return Velocity.Zero
            onSettle(available.y)
            return available
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (progress >= 1f || progress <= 0f || abs(available.y) < 1f) return Velocity.Zero
            onSettle(available.y)
            return available
        }
    }

    private companion object {
        const val FLING_VELOCITY = 1200f
        const val SETTLE_THRESHOLD = 0.45f
        const val OPEN_THRESHOLD = 0.98f
    }
}
