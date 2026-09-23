package com.abrarshakhi.bricks.feature.drawer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.bricks.core.ui.drawer.LauncherDrawerState
import com.abrarshakhi.bricks.core.ui.mvi.CollectEffects
import com.abrarshakhi.bricks.feature.drawer.DrawerEffect
import com.abrarshakhi.bricks.feature.drawer.DrawerMessage
import com.abrarshakhi.bricks.feature.drawer.DrawerViewModel
import com.abrarshakhi.bricks.feature.drawer.R
import com.abrarshakhi.paperui.PuiSnackbarHostState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DrawerRoute(
    drawerState: LauncherDrawerState,
    onOpenSettings: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    viewModel: DrawerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { PuiSnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (drawerState.isVisible) scope.launch { drawerState.snapClosed() }
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            DrawerEffect.Close -> scope.launch { drawerState.close() }
            is DrawerEffect.CopyText -> context.copyToClipboard(effect.text)
            is DrawerEffect.ShowMessage -> scope.launch { snackbar.showSnackbar(context.messageText(effect.message)) }
        }
    }

    AppDrawer(
        state = state,
        onIntent = viewModel::onIntent,
        drawerState = drawerState,
        snackbar = snackbar,
        onOpenSettings = onOpenSettings,
        onOpenHiddenApps = onOpenHiddenApps,
    )
}

private fun Context.copyToClipboard(text: String) {
    getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(getString(R.string.drawer_clip_label), text))
}

private fun Context.messageText(message: DrawerMessage): String = getString(
    when (message) {
        DrawerMessage.LaunchFailed -> R.string.drawer_msg_launch_failed
        DrawerMessage.AddedToHome -> R.string.drawer_msg_added_home
        DrawerMessage.AddedToDock -> R.string.drawer_msg_added_dock
        DrawerMessage.DockFull -> R.string.drawer_msg_dock_full
        DrawerMessage.AppHidden -> R.string.drawer_msg_hidden
        DrawerMessage.CannotUninstall -> R.string.drawer_msg_cannot_uninstall
        DrawerMessage.Copied -> R.string.drawer_msg_copied
    },
)
