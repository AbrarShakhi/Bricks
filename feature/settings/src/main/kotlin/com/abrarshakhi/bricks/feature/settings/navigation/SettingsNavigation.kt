package com.abrarshakhi.bricks.feature.settings.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.abrarshakhi.bricks.core.ui.navigation.PaneMetadata
import com.abrarshakhi.bricks.feature.settings.HiddenAppsViewModel
import com.abrarshakhi.bricks.feature.settings.SettingsIntent
import com.abrarshakhi.bricks.feature.settings.SettingsViewModel
import com.abrarshakhi.bricks.feature.settings.ui.AboutScreen
import com.abrarshakhi.bricks.feature.settings.ui.AppearanceScreen
import com.abrarshakhi.bricks.feature.settings.ui.DrawerSettingsScreen
import com.abrarshakhi.bricks.feature.settings.ui.GestureSettingsScreen
import com.abrarshakhi.bricks.feature.settings.ui.HiddenAppsScreen
import com.abrarshakhi.bricks.feature.settings.ui.HomeSettingsScreen
import com.abrarshakhi.bricks.feature.settings.ui.SettingsPage
import com.abrarshakhi.bricks.feature.settings.ui.SettingsPlaceholder
import com.abrarshakhi.bricks.feature.settings.ui.SettingsRootScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object SettingsRootKey : NavKey

@Serializable
data class SettingsPageKey(val page: SettingsPage) : NavKey

fun EntryProviderScope<NavKey>.settingsEntries(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
) {
    entry<SettingsRootKey>(metadata = PaneMetadata.List) {
        val viewModel: SettingsViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LifecycleResumeEffect(viewModel) {
            viewModel.onIntent(SettingsIntent.Resumed)
            onPauseOrDispose { }
        }
        SettingsRootScreen(
            state = state,
            onIntent = viewModel::onIntent,
            onOpen = { page -> onNavigate(SettingsPageKey(page)) },
            onBack = onBack,
        )
    }
    entry<SettingsPageKey>(metadata = PaneMetadata.Detail) { key ->
        if (key.page == SettingsPage.HiddenApps) {
            val viewModel: HiddenAppsViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            HiddenAppsScreen(
                state = state,
                iconStyle = state.iconStyle,
                onIntent = viewModel::onIntent,
                onBack = onBack,
            )
        } else {
            val viewModel: SettingsViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            LifecycleResumeEffect(viewModel) {
                viewModel.onIntent(SettingsIntent.Resumed)
                onPauseOrDispose { }
            }
            when (key.page) {
                SettingsPage.Appearance -> AppearanceScreen(state, viewModel::onIntent, onBack)
                SettingsPage.Home -> HomeSettingsScreen(state, viewModel::onIntent, onBack)
                SettingsPage.Drawer -> DrawerSettingsScreen(state, viewModel::onIntent, onBack)
                SettingsPage.Gestures -> GestureSettingsScreen(state, viewModel::onIntent, onBack)
                SettingsPage.About -> AboutScreen(state, viewModel::onIntent, onBack)
                SettingsPage.HiddenApps -> SettingsPlaceholder()
            }
        }
    }
}
