package com.abrarshakhi.bricks.feature.widgets.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.bricks.core.ui.mvi.CollectEffects
import com.abrarshakhi.bricks.core.ui.util.findActivity
import com.abrarshakhi.bricks.core.ui.widget.ActivityResultBridge
import com.abrarshakhi.bricks.core.ui.widget.LauncherWidgetHost
import com.abrarshakhi.bricks.feature.widgets.R
import com.abrarshakhi.bricks.feature.widgets.WidgetsEffect
import com.abrarshakhi.bricks.feature.widgets.WidgetsIntent
import com.abrarshakhi.bricks.feature.widgets.WidgetsMessage
import com.abrarshakhi.bricks.feature.widgets.WidgetsViewModel
import com.abrarshakhi.paperui.PuiSnackbarHostState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WidgetsRoute(
    onBack: () -> Unit,
    viewModel: WidgetsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val host = koinInject<LauncherWidgetHost>()
    val bridge = koinInject<ActivityResultBridge>()
    val snackbar = remember { PuiSnackbarHostState() }
    val scope = rememberCoroutineScope()
    val shortSide = minOf(configuration.screenWidthDp, configuration.screenHeightDp).toFloat()
    val longSide = maxOf(configuration.screenWidthDp, configuration.screenHeightDp).toFloat()
    val cellWidth = (shortSide - HORIZONTAL_PADDING_DP) / state.grid.columns
    val cellHeight = longSide * GRID_HEIGHT_FRACTION / state.grid.rows
    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.onIntent(WidgetsIntent.BindResult(result.resultCode == Activity.RESULT_OK))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is WidgetsEffect.RequestBind -> runCatching { bindLauncher.launch(effect.intent) }
                .onFailure { viewModel.onIntent(WidgetsIntent.BindResult(false)) }
            is WidgetsEffect.Configure -> {
                val activity = context.findActivity()
                val started = activity != null && host.startConfiguration(activity, effect.appWidgetId, effect.requestCode)
                if (!started) bridge.dispatch(effect.requestCode, Activity.RESULT_CANCELED)
            }
            WidgetsEffect.Added -> onBack()
            is WidgetsEffect.ShowMessage -> scope.launch { snackbar.showSnackbar(context.messageText(effect.message)) }
        }
    }

    WidgetPickerScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        cellWidthDp = cellWidth,
        cellHeightDp = cellHeight,
        snackbar = snackbar,
    )
}

private fun Context.messageText(message: WidgetsMessage): String = getString(
    when (message) {
        WidgetsMessage.Failed -> R.string.widgets_msg_failed
        WidgetsMessage.Cancelled -> R.string.widgets_msg_cancelled
    },
)

private const val HORIZONTAL_PADDING_DP = 24f
private const val GRID_HEIGHT_FRACTION = 0.62f
