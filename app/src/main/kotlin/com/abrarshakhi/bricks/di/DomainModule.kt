package com.abrarshakhi.bricks.di

import com.abrarshakhi.bricks.core.domain.search.AppSearchEngine
import com.abrarshakhi.bricks.core.domain.search.ExpressionEvaluator
import com.abrarshakhi.bricks.core.domain.usecase.apps.EvaluateExpressionUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.LaunchAppUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveAllAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveHiddenAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveProfilesUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveSuggestedAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.ObserveVisibleAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.OpenAppInfoUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.OpenStoreListingUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.RenameAppUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.SearchAppsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.SearchExternallyUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.SetAppHiddenUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.SetProfilePausedUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.UninstallAppUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.AddAppToDockUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.AddAppToHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.AddHomePageUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.AddWidgetToHomeUseCase
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
import com.abrarshakhi.bricks.core.domain.usecase.home.SynchronizeHomeLayoutUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ChangeDockSlotsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ChangeHomeGridUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.EnsureHomeGridUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ObserveSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.UpdateSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.GetAppShortcutsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.LaunchShortcutUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveGlanceUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveNotificationAccessUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveNotificationBadgesUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveWidgetProvidersUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.PerformGestureUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.SystemStatusUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {
    single { AppSearchEngine() }
    single { ExpressionEvaluator() }

    factoryOf(::ObserveVisibleAppsUseCase)
    factoryOf(::ObserveAllAppsUseCase)
    factoryOf(::ObserveHiddenAppsUseCase)
    factoryOf(::ObserveSuggestedAppsUseCase)
    factoryOf(::SearchAppsUseCase)
    factoryOf(::EvaluateExpressionUseCase)
    factoryOf(::LaunchAppUseCase)
    factoryOf(::OpenAppInfoUseCase)
    factoryOf(::UninstallAppUseCase)
    factoryOf(::OpenStoreListingUseCase)
    factoryOf(::SearchExternallyUseCase)
    factoryOf(::SetAppHiddenUseCase)
    factoryOf(::RenameAppUseCase)
    factoryOf(::ObserveProfilesUseCase)
    factoryOf(::SetProfilePausedUseCase)

    factoryOf(::ObserveHomeLayoutUseCase)
    factoryOf(::AddAppToHomeUseCase)
    factoryOf(::AddAppToDockUseCase)
    factoryOf(::PinShortcutToHomeUseCase)
    factoryOf(::FindWidgetProviderUseCase)
    factoryOf(::AddWidgetToHomeUseCase)
    factoryOf(::DropAppOnHomeUseCase)
    factoryOf(::MoveHomeItemUseCase)
    factoryOf(::RemoveHomeItemUseCase)
    factoryOf(::RenameFolderUseCase)
    factoryOf(::ResizeWidgetUseCase)
    factoryOf(::AddHomePageUseCase)
    factoryOf(::RemoveHomePageUseCase)
    factoryOf(::CompactHomePagesUseCase)
    factoryOf(::SynchronizeHomeLayoutUseCase)

    factoryOf(::ObserveSettingsUseCase)
    factoryOf(::UpdateSettingsUseCase)
    factoryOf(::EnsureHomeGridUseCase)
    factoryOf(::ChangeHomeGridUseCase)
    factoryOf(::ChangeDockSlotsUseCase)

    factoryOf(::PerformGestureUseCase)
    factoryOf(::ObserveGlanceUseCase)
    factoryOf(::ObserveNotificationBadgesUseCase)
    factoryOf(::ObserveNotificationAccessUseCase)
    factoryOf(::GetAppShortcutsUseCase)
    factoryOf(::LaunchShortcutUseCase)
    factoryOf(::ObserveWidgetProvidersUseCase)
    factoryOf(::SystemStatusUseCase)
}
