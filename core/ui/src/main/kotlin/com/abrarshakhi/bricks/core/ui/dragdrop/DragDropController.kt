package com.abrarshakhi.bricks.core.ui.dragdrop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.DpSize
import com.abrarshakhi.bricks.core.domain.model.AppKey

sealed interface DragPayload {
    data class HomeItem(val itemId: Long, val fromFolder: Boolean) : DragPayload
    data class DrawerApp(val app: AppKey) : DragPayload
}

@Stable
class DragSession internal constructor(
    val payload: DragPayload,
    val grabOffset: Offset,
    val ghostSize: DpSize,
    val ghost: @Composable () -> Unit,
)

@Stable
class DragDropController {
    var session: DragSession? by mutableStateOf(null)
        private set

    var pointer: Offset by mutableStateOf(Offset.Unspecified)
        private set

    val isDragging: Boolean
        get() = session != null

    fun start(
        payload: DragPayload,
        rootPosition: Offset,
        grabOffset: Offset,
        ghostSize: DpSize,
        ghost: @Composable () -> Unit,
    ) {
        pointer = rootPosition
        session = DragSession(payload, grabOffset, ghostSize, ghost)
    }

    fun move(rootPosition: Offset) {
        if (session != null) pointer = rootPosition
    }

    fun finish(): DragDrop? {
        val active = session ?: return null
        val position = pointer
        session = null
        pointer = Offset.Unspecified
        return DragDrop(active.payload, position)
    }

    fun cancel() {
        session = null
        pointer = Offset.Unspecified
    }
}

data class DragDrop(
    val payload: DragPayload,
    val rootPosition: Offset,
)

val LocalDragDropController = staticCompositionLocalOf { DragDropController() }

@Composable
fun DragGhostLayer(controller: DragDropController, modifier: Modifier = Modifier) {
    val layer = remember { LayerOrigin() }
    Box(modifier = modifier.onGloballyPositioned { layer.coordinates = it }) {
        val session = controller.session ?: return@Box
        val lift = remember(session) { Animatable(0f) }
        LaunchedEffect(session) {
            lift.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        Box(
            modifier = Modifier
                .size(session.ghostSize)
                .graphicsLayer {
                    val position = controller.pointer
                    val origin = layer.coordinates?.takeIf { it.isAttached }?.rootToLocal(position) ?: position
                    if (position != Offset.Unspecified) {
                        translationX = origin.x - session.grabOffset.x
                        translationY = origin.y - session.grabOffset.y
                    }
                    val progress = lift.value
                    scaleX = 1f + GHOST_SCALE * progress
                    scaleY = 1f + GHOST_SCALE * progress
                    rotationZ = GHOST_TILT * progress
                },
        ) {
            session.ghost()
        }
    }
}

private class LayerOrigin {
    var coordinates: LayoutCoordinates? = null
}

fun LayoutCoordinates.rootToLocal(rootPosition: Offset): Offset =
    localPositionOf(findRootCoordinates(), rootPosition)

fun LayoutCoordinates.containsRoot(rootPosition: Offset): Boolean {
    if (!isAttached || rootPosition == Offset.Unspecified) return false
    val local = rootToLocal(rootPosition)
    return local.x >= 0f && local.y >= 0f && local.x <= size.width && local.y <= size.height
}

private const val GHOST_SCALE = 0.12f
private const val GHOST_TILT = -5f
