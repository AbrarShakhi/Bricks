package com.abrarshakhi.bricks.core.ui.util

import android.app.Activity
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun WallpaperScrollEffect(pagerState: PagerState, pageCount: Int, enabled: Boolean) {
    val view = LocalView.current
    val manager = remember(view) { WallpaperManager.getInstance(view.context) }
    LaunchedEffect(pagerState, pageCount, enabled, manager) {
        val steps = if (pageCount > 1) 1f / (pageCount - 1) else 1f
        runCatching { manager.setWallpaperOffsetSteps(steps, 1f) }
        if (!enabled || pageCount <= 1) {
            view.windowToken?.let { token -> runCatching { manager.setWallpaperOffsets(token, 0.5f, 0.5f) } }
            return@LaunchedEffect
        }
        snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }
            .collect { position ->
                val token = view.windowToken ?: return@collect
                val x = (position / (pageCount - 1)).coerceIn(0f, 1f)
                runCatching { manager.setWallpaperOffsets(token, x, 0.5f) }
            }
    }
}

@Composable
fun rememberWallpaperIsLight(): Boolean {
    val context = LocalContext.current
    var light by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            return@DisposableEffect onDispose { }
        }
        val manager = WallpaperManager.getInstance(context)
        fun read() {
            val colors = runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
            light = colors?.isLight() == true
        }
        read()
        val listener = WallpaperManager.OnColorsChangedListener { _, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) read()
        }
        runCatching { manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper())) }
        onDispose { runCatching { manager.removeOnColorsChangedListener(listener) } }
    }
    return light
}

private fun WallpaperColors.isLight(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return (colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return false
    return primaryColor.luminance() > 0.5f
}

@Composable
fun SystemBarsAppearance(lightContent: Boolean) {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    DisposableEffect(view, lightContent, configuration) {
        view.context.findActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !lightContent
                isAppearanceLightNavigationBars = !lightContent
            }
        }
        onDispose { }
    }
}

fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
