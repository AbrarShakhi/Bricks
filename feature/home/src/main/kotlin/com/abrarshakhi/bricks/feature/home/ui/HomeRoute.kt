package com.abrarshakhi.bricks.feature.home.ui

import android.content.Context
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.bricks.core.domain.layout.HomeLayoutEditor
import com.abrarshakhi.bricks.core.ui.dragdrop.DragDropController
import com.abrarshakhi.bricks.core.ui.dragdrop.LocalDragDropController
import com.abrarshakhi.bricks.core.ui.drawer.LauncherDrawerState
import com.abrarshakhi.bricks.core.ui.mvi.CollectEffects
import com.abrarshakhi.bricks.core.ui.util.HomeButtonEvents
import com.abrarshakhi.bricks.core.ui.util.findActivity
import com.abrarshakhi.bricks.core.ui.widget.LauncherWidgetHost
import com.abrarshakhi.bricks.feature.home.HomeEffect
import com.abrarshakhi.bricks.feature.home.HomeIntent
import com.abrarshakhi.bricks.feature.home.HomeMessage
import com.abrarshakhi.bricks.feature.home.HomeViewModel
import com.abrarshakhi.bricks.feature.home.R
import com.abrarshakhi.paperui.PuiSnackbarDuration
import com.abrarshakhi.paperui.PuiSnackbarHostState
import com.abrarshakhi.paperui.PuiSnackbarResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute(
    drawerState: LauncherDrawerState,
    dragController: DragDropController,
    onOpenSettings: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    drawerContent: @Composable () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val latestState by rememberUpdatedState(state)
    val snackbar = remember { PuiSnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val homeButtons = koinInject<HomeButtonEvents>()
    val widgetHost = koinInject<LauncherWidgetHost>()
    val pagerState = rememberPagerState {
        val pages = latestState.pageCount
        if (dragController.isDragging && pages < HomeLayoutEditor.MAX_PAGES) pages + 1 else pages
    }

    LaunchedEffect(configuration.smallestScreenWidthDp) {
        viewModel.onIntent(
            HomeIntent.DeviceMetrics(
                smallestWidthDp = configuration.smallestScreenWidthDp,
                largestWidthDp = maxOf(configuration.screenWidthDp, configuration.screenHeightDp),
            ),
        )
    }

    LaunchedEffect(homeButtons) {
        homeButtons.presses.collect { alreadyVisible ->
            viewModel.onIntent(HomeIntent.HomePressed(alreadyVisible))
        }
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(HomeIntent.Resumed)
        onPauseOrDispose { dragController.cancel() }
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is HomeEffect.OpenDrawer -> {
                drawerState.requestOpen(effect.focusSearch)
                scope.launch { drawerState.open() }
            }
            HomeEffect.OpenSettings -> onOpenSettings()
            HomeEffect.OpenWidgetPicker -> onOpenWidgetPicker()
            is HomeEffect.ReconfigureWidget -> {
                context.findActivity()?.let { activity ->
                    widgetHost.startConfiguration(activity, effect.appWidgetId, RECONFIGURE_REQUEST_CODE)
                }
            }
            is HomeEffect.ResetHome -> {
                dragController.cancel()
                scope.launch {
                    when {
                        drawerState.isVisible && effect.scrollToFirstPage -> drawerState.close()
                        drawerState.isVisible -> drawerState.snapClosed()
                        effect.scrollToFirstPage && pagerState.currentPage != 0 -> pagerState.animateScrollToPage(0)
                    }
                }
            }
            is HomeEffect.ScrollToPage -> scope.launch {
                withTimeoutOrNull(PAGE_WAIT_MILLIS) { snapshotFlow { pagerState.pageCount }.first { it > effect.page } }
                pagerState.animateScrollToPage(effect.page)
            }
            is HomeEffect.ShowMessage -> scope.launch {
                val action = if (effect.message == HomeMessage.LockScreenSetup) {
                    context.getString(R.string.home_msg_lock_setup_action)
                } else {
                    null
                }
                val result = snackbar.showSnackbar(
                    message = context.messageText(effect.message),
                    actionLabel = action,
                    duration = if (action != null) PuiSnackbarDuration.Long else PuiSnackbarDuration.Short,
                )
                if (result == PuiSnackbarResult.ActionPerformed) viewModel.onIntent(HomeIntent.OpenLockScreenSetup)
            }
        }
    }

    CompositionLocalProvider(LocalDragDropController provides dragController) {
        HomeScreen(
            state = state,
            onIntent = viewModel::onIntent,
            drawerState = drawerState,
            pagerState = pagerState,
            dragController = dragController,
            snackbarHost = snackbar,
            drawerContent = drawerContent,
        )
    }
}

private fun Context.messageText(message: HomeMessage): String = getString(
    when (message) {
        HomeMessage.LaunchFailed -> R.string.home_msg_launch_failed
        HomeMessage.DockFull -> R.string.home_msg_dock_full
        HomeMessage.NoRoomForWidget -> R.string.home_msg_no_room
        HomeMessage.LockScreenSetup -> R.string.home_msg_lock_setup
        HomeMessage.GestureUnavailable -> R.string.home_msg_gesture_unavailable
        HomeMessage.AppHidden -> R.string.home_msg_app_hidden
        HomeMessage.CannotUninstall -> R.string.home_msg_cannot_uninstall
    },
)

private const val RECONFIGURE_REQUEST_CODE = 7301
private const val PAGE_WAIT_MILLIS = 1_000L
