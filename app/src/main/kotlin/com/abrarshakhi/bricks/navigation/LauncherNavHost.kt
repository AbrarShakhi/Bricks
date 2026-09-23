package com.abrarshakhi.bricks.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.abrarshakhi.bricks.R
import com.abrarshakhi.bricks.core.designsystem.component.EmptyState
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.ui.dragdrop.DragDropController
import com.abrarshakhi.bricks.core.ui.drawer.LauncherDrawerState
import com.abrarshakhi.bricks.core.ui.util.HomeButtonEvents
import com.abrarshakhi.bricks.core.ui.util.SystemBarsAppearance
import com.abrarshakhi.bricks.feature.drawer.ui.DrawerRoute
import com.abrarshakhi.bricks.feature.home.navigation.HomeRouteKey
import com.abrarshakhi.bricks.feature.home.ui.HomeRoute
import com.abrarshakhi.bricks.feature.settings.navigation.SettingsPageKey
import com.abrarshakhi.bricks.feature.settings.navigation.SettingsRootKey
import com.abrarshakhi.bricks.feature.settings.navigation.settingsEntries
import com.abrarshakhi.bricks.feature.settings.ui.SettingsPage
import com.abrarshakhi.bricks.feature.widgets.navigation.WidgetPickerKey
import com.abrarshakhi.bricks.feature.widgets.navigation.widgetEntries
import org.koin.compose.koinInject

@Composable
fun LauncherNavHost(darkTheme: Boolean) {
    val backStack = rememberNavBackStack(HomeRouteKey)
    val homeButtons = koinInject<HomeButtonEvents>()
    val drawerState = remember { LauncherDrawerState() }
    val dragController = remember { DragDropController() }

    LaunchedEffect(homeButtons, backStack) {
        homeButtons.presses.collect {
            while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        }
    }

    fun navigate(key: NavKey) {
        val top = backStack.lastOrNull()
        when {
            top == key -> Unit
            top is SettingsPageKey && key is SettingsPageKey -> backStack[backStack.lastIndex] = key
            else -> backStack.add(key)
        }
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    if (backStack.lastOrNull() != HomeRouteKey) {
        SystemBarsAppearance(lightContent = darkTheme)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= TWO_PANE_WIDTH
        val placeholderTitle = stringResource(R.string.app_pick_section_title)
        val placeholderMessage = stringResource(R.string.app_pick_section_message)
        val sceneStrategy = remember(wide, placeholderTitle, placeholderMessage) {
            ListDetailSceneStrategy<NavKey>(enabled = wide) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(title = placeholderTitle, message = placeholderMessage, icon = BricksIcons.Settings)
                }
            }
        }
        NavDisplay(
            backStack = backStack,
            onBack = { back() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            sceneStrategies = listOf(sceneStrategy),
            transitionSpec = {
                (slideInHorizontally(tween(TRANSITION_MILLIS)) { it / 4 } + fadeIn(tween(TRANSITION_MILLIS))) togetherWith
                    fadeOut(tween(FADE_MILLIS))
            },
            popTransitionSpec = {
                fadeIn(tween(FADE_MILLIS)) togetherWith
                    (slideOutHorizontally(tween(TRANSITION_MILLIS)) { it / 4 } + fadeOut(tween(TRANSITION_MILLIS)))
            },
            predictivePopTransitionSpec = {
                fadeIn(tween(FADE_MILLIS)) togetherWith
                    (slideOutHorizontally(tween(TRANSITION_MILLIS)) { it / 4 } + fadeOut(tween(TRANSITION_MILLIS)))
            },
            entryProvider = entryProvider {
                entry<HomeRouteKey> {
                    HomeRoute(
                        drawerState = drawerState,
                        dragController = dragController,
                        onOpenSettings = { navigate(SettingsRootKey) },
                        onOpenWidgetPicker = { navigate(WidgetPickerKey) },
                        drawerContent = {
                            DrawerRoute(
                                drawerState = drawerState,
                                onOpenSettings = { navigate(SettingsRootKey) },
                                onOpenHiddenApps = { navigate(SettingsPageKey(SettingsPage.HiddenApps)) },
                            )
                        },
                    )
                }
                settingsEntries(onNavigate = ::navigate, onBack = ::back)
                widgetEntries(onBack = ::back)
            },
        )
    }
}

private val TWO_PANE_WIDTH = 840.dp
private const val TRANSITION_MILLIS = 260
private const val FADE_MILLIS = 180
