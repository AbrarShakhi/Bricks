package com.abrarshakhi.bricks.core.ui.icon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.IconStyle
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun rememberAppIcon(app: AppKey, size: Dp, style: IconStyle): LoadedIcon? {
    val loader = koinInject<AppIconLoader>()
    val sizePx = with(LocalDensity.current) { size.toPx().roundToInt() }
    val version by loader.version.collectAsState()
    val icon by produceState(loader.peek(app, sizePx, style), app, sizePx, style, version) {
        value = loader.load(app, sizePx, style)
    }
    return icon
}

@Composable
fun rememberShortcutIcon(shortcut: ShortcutKey, fallbackApp: AppKey?, size: Dp, style: IconStyle): LoadedIcon? {
    val loader = koinInject<AppIconLoader>()
    val sizePx = with(LocalDensity.current) { size.toPx().roundToInt() }
    val version by loader.version.collectAsState()
    val icon by produceState(loader.peekShortcut(shortcut, sizePx, style), shortcut, fallbackApp, sizePx, style, version) {
        value = loader.loadShortcut(shortcut, sizePx, style)
            ?: fallbackApp?.let { loader.load(it, sizePx, style) }
    }
    return icon
}
