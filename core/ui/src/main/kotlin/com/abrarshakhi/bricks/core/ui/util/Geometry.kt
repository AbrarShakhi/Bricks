package com.abrarshakhi.bricks.core.ui.util

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds
import kotlin.math.roundToInt

fun LayoutCoordinates.screenBounds(): ScreenBounds? {
    if (!isAttached) return null
    val bounds = boundsInWindow()
    return ScreenBounds(
        left = bounds.left.roundToInt(),
        top = bounds.top.roundToInt(),
        right = bounds.right.roundToInt(),
        bottom = bounds.bottom.roundToInt(),
    )
}

class CoordinatesHolder {
    var coordinates: LayoutCoordinates? = null

    val bounds: ScreenBounds?
        get() = coordinates?.screenBounds()
}
