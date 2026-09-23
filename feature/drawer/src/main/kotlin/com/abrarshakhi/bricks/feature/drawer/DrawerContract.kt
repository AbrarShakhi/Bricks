package com.abrarshakhi.bricks.feature.drawer

import androidx.compose.runtime.Immutable
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppShortcut
import com.abrarshakhi.bricks.core.domain.model.AppSort
import com.abrarshakhi.bricks.core.domain.model.AppearanceSettings
import com.abrarshakhi.bricks.core.domain.model.DrawerSettings
import com.abrarshakhi.bricks.core.domain.model.ProfileType
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds
import com.abrarshakhi.bricks.core.domain.usecase.apps.CalculatorResult

@Immutable
data class DrawerApp(
    val key: AppKey,
    val label: String,
    val systemLabel: String,
    val profile: ProfileType,
    val isPaused: Boolean,
    val isSystem: Boolean,
    val badge: Int,
    val section: String,
)

@Immutable
sealed interface DrawerRow {
    val key: String

    @Immutable
    data class Header(val letter: String) : DrawerRow {
        override val key: String
            get() = "header:$letter"
    }

    @Immutable
    data class Item(val app: DrawerApp) : DrawerRow {
        override val key: String
            get() = app.key.flattened
    }
}

@Immutable
data class DrawerSection(
    val letter: String,
    val rowIndex: Int,
)

@Immutable
data class DrawerMenu(
    val app: AppKey,
    val shortcuts: List<AppShortcut> = emptyList(),
    val loadingShortcuts: Boolean = true,
)

@Immutable
data class DrawerRename(
    val app: AppKey,
    val label: String,
    val systemLabel: String,
)

@Immutable
data class DrawerState(
    val ready: Boolean = false,
    val appearance: AppearanceSettings = AppearanceSettings(),
    val drawer: DrawerSettings = DrawerSettings(),
    val profiles: List<ProfileType> = listOf(ProfileType.Personal),
    val selectedProfile: ProfileType = ProfileType.Personal,
    val workPaused: Boolean = false,
    val rows: List<DrawerRow> = emptyList(),
    val sections: List<DrawerSection> = emptyList(),
    val suggestions: List<DrawerApp> = emptyList(),
    val query: String = "",
    val results: List<DrawerApp> = emptyList(),
    val calculator: CalculatorResult? = null,
    val menu: DrawerMenu? = null,
    val rename: DrawerRename? = null,
) {
    val isSearching: Boolean
        get() = query.isNotBlank()

    val hasWorkProfile: Boolean
        get() = ProfileType.Work in profiles
}

enum class DrawerAppAction { AppInfo, AddToHome, AddToDock, Hide, Rename, Uninstall, Store }

sealed interface DrawerIntent {
    data class QueryChanged(val query: String) : DrawerIntent
    data object SubmitSearch : DrawerIntent
    data class SelectProfile(val profile: ProfileType) : DrawerIntent
    data class SetWorkPaused(val paused: Boolean) : DrawerIntent
    data class Launch(val app: AppKey, val bounds: ScreenBounds?) : DrawerIntent
    data class ShowMenu(val app: AppKey) : DrawerIntent
    data object DismissMenu : DrawerIntent
    data class MenuAction(val app: AppKey, val action: DrawerAppAction) : DrawerIntent
    data class LaunchShortcut(val shortcut: AppShortcut, val bounds: ScreenBounds?) : DrawerIntent
    data class PinShortcut(val shortcut: AppShortcut) : DrawerIntent
    data class Rename(val app: AppKey, val label: String?) : DrawerIntent
    data object DismissRename : DrawerIntent
    data object SearchWeb : DrawerIntent
    data object SearchStore : DrawerIntent
    data object CopyCalculation : DrawerIntent
    data class ChangeSort(val sort: AppSort) : DrawerIntent
    data object Closed : DrawerIntent
}

enum class DrawerMessage { LaunchFailed, AddedToHome, AddedToDock, DockFull, AppHidden, CannotUninstall, Copied }

sealed interface DrawerEffect {
    data object Close : DrawerEffect
    data class ShowMessage(val message: DrawerMessage) : DrawerEffect
    data class CopyText(val text: String) : DrawerEffect
}
