package com.abrarshakhi.bricks.core.ui.widget

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.ui.R
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme
import org.koin.compose.koinInject

@Composable
fun AppWidget(
    appWidgetId: Int,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit,
) {
    key(appWidgetId) {
        val host = koinInject<LauncherWidgetHost>()
        val available = remember { host.info(appWidgetId) != null }
        val latestLongPress by rememberUpdatedState(onLongPress)
        val density = LocalDensity.current
        val reference = remember { WidgetViewReference() }
        if (!available) {
            WidgetUnavailable(modifier)
            return@key
        }
        AndroidView(
            factory = { context ->
                host.createView(context, appWidgetId)
                    ?.also { view ->
                        view.onLongPress = { latestLongPress() }
                        reference.view = view
                    }
                    ?: FrameLayout(context)
            },
            modifier = modifier.onSizeChanged { size ->
                with(density) {
                    reference.view?.updateSizeDp(size.width.toDp().value, size.height.toDp().value)
                }
            },
            update = { view -> (view as? LauncherWidgetHostView)?.onLongPress = { latestLongPress() } },
            onRelease = { view ->
                (view as? LauncherWidgetHostView)?.onLongPress = null
                reference.view = null
            },
        )
    }
}

private class WidgetViewReference {
    var view: LauncherWidgetHostView? = null
}

@Composable
private fun WidgetUnavailable(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(PuiTheme.spacing.md),
        ) {
            PuiIcon(BricksIcons.Widgets, contentDescription = null, tint = PuiTheme.colors.onSurfaceMuted)
            PuiText(
                text = stringResource(R.string.ui_widget_unavailable),
                style = PuiTheme.typography.labelMedium,
                color = PuiTheme.colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
