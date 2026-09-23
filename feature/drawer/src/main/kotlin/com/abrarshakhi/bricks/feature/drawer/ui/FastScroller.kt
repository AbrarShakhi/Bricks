package com.abrarshakhi.bricks.feature.drawer.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.abrarshakhi.bricks.feature.drawer.DrawerSection
import com.abrarshakhi.paperui.PuiBubbleTail
import com.abrarshakhi.paperui.PuiSpeechBubble
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme
import kotlin.math.roundToInt

@Composable
internal fun FastScroller(
    sections: List<DrawerSection>,
    onSectionSelected: (DrawerSection) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    if (sections.size < 2) return
    val haptics = LocalHapticFeedback.current
    val latestSections by rememberUpdatedState(sections)
    val latestSelect by rememberUpdatedState(onSectionSelected)
    var active by remember { mutableStateOf<String?>(null) }
    var height by remember { mutableIntStateOf(0) }
    var touchY by remember { mutableFloatStateOf(0f) }

    fun select(y: Float) {
        val list = latestSections
        if (list.isEmpty() || height <= 0) return
        touchY = y.coerceIn(0f, height.toFloat())
        val index = (touchY / height * list.size).toInt().coerceIn(0, list.lastIndex)
        val section = list[index]
        if (active != section.letter) {
            active = section.letter
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            latestSelect(section)
        }
    }

    Box(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(SCROLLER_WIDTH)
                .onSizeChanged { height = it.height }
                .semantics { contentDescription = description }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        select(down.position.y)
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            select(change.position.y)
                            change.consume()
                        }
                        active = null
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            sections.forEach { section ->
                PuiText(
                    text = section.letter,
                    style = PuiTheme.typography.labelSmall,
                    color = if (active == section.letter) PuiTheme.colors.primary else PuiTheme.colors.onSurfaceMuted,
                )
            }
        }
        val letter = active
        if (letter != null) {
            PuiSpeechBubble(
                modifier = Modifier
                    .zIndex(1f)
                    .offset { IntOffset(-BUBBLE_OFFSET.roundToPx(), (touchY - BUBBLE_HALF_HEIGHT.toPx()).roundToInt()) }
                    .padding(end = PuiTheme.spacing.xs),
                tail = PuiBubbleTail.End,
                tailBias = 0.5f,
                color = PuiTheme.colors.accent,
            ) {
                PuiText(
                    text = letter,
                    style = PuiTheme.typography.headlineLarge,
                    color = PuiTheme.colors.onAccent,
                )
            }
        }
    }
}

internal val SCROLLER_WIDTH = 28.dp
private val BUBBLE_OFFSET = 92.dp
private val BUBBLE_HALF_HEIGHT = 34.dp
