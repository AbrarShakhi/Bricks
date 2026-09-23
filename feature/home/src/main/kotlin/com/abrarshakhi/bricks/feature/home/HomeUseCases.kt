package com.abrarshakhi.bricks.feature.home

import com.abrarshakhi.bricks.core.domain.usecase.apps.LaunchAppUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveAllAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.OpenAppInfoUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.RenameAppUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.SetAppHiddenUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.UninstallAppUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.AddHomePageUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.CompactHomePagesUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.DropAppOnHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.FindWidgetProviderUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.MoveHomeItemUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.ObserveHomeLayoutUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.PinShortcutToHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.RemoveHomeItemUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.RemoveHomePageUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.RenameFolderUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.ResizeWidgetUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.EnsureHomeGridUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ObserveSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.GetAppShortcutsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.LaunchShortcutUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveGlanceUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveNotificationBadgesUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.PerformGestureUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.SystemStatusUseCase

class HomeObservers(
    val layout: ObserveHomeLayoutUseCase,
    val apps: ObserveAllAppsUseCase,
    val settings: ObserveSettingsUseCase,
    val badges: ObserveNotificationBadgesUseCase,
    val glance: ObserveGlanceUseCase,
)

class HomeAppActions(
    val launchApp: LaunchAppUseCase,
    val launchShortcut: LaunchShortcutUseCase,
    val shortcuts: GetAppShortcutsUseCase,
    val appInfo: OpenAppInfoUseCase,
    val uninstall: UninstallAppUseCase,
    val setHidden: SetAppHiddenUseCase,
    val rename: RenameAppUseCase,
)

class HomeLayoutActions(
    val ensureGrid: EnsureHomeGridUseCase,
    val move: MoveHomeItemUseCase,
    val dropApp: DropAppOnHomeUseCase,
    val remove: RemoveHomeItemUseCase,
    val renameFolder: RenameFolderUseCase,
    val resizeWidget: ResizeWidgetUseCase,
    val findWidgetProvider: FindWidgetProviderUseCase,
    val addPage: AddHomePageUseCase,
    val removePage: RemoveHomePageUseCase,
    val compactPages: CompactHomePagesUseCase,
    val pinShortcut: PinShortcutToHomeUseCase,
)

class HomeSystemActions(
    val gesture: PerformGestureUseCase,
    val status: SystemStatusUseCase,
)
