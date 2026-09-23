package com.abrarshakhi.bricks.feature.drawer

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppSort
import com.abrarshakhi.bricks.core.domain.model.LauncherSettings
import com.abrarshakhi.bricks.core.domain.model.PackageUserKey
import com.abrarshakhi.bricks.core.domain.model.ProfileType
import com.abrarshakhi.bricks.core.domain.model.UserProfile
import com.abrarshakhi.bricks.core.domain.search.AppSearchEngine
import com.abrarshakhi.bricks.core.domain.usecase.apps.EvaluateExpressionUseCase
import com.abrarshakhi.bricks.core.domain.usecase.apps.LaunchAppUseCase
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
import com.abrarshakhi.bricks.core.domain.usecase.home.PinShortcutToHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ObserveSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.UpdateSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.GetAppShortcutsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.LaunchShortcutUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveNotificationBadgesUseCase
import com.abrarshakhi.bricks.core.ui.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DrawerObservers(
    val visibleApps: ObserveVisibleAppsUseCase,
    val suggested: ObserveSuggestedAppsUseCase,
    val profiles: ObserveProfilesUseCase,
    val settings: ObserveSettingsUseCase,
    val badges: ObserveNotificationBadgesUseCase,
)

class DrawerActions(
    val launch: LaunchAppUseCase,
    val search: SearchAppsUseCase,
    val evaluate: EvaluateExpressionUseCase,
    val shortcuts: GetAppShortcutsUseCase,
    val launchShortcut: LaunchShortcutUseCase,
    val pinShortcut: PinShortcutToHomeUseCase,
    val appInfo: OpenAppInfoUseCase,
    val uninstall: UninstallAppUseCase,
    val setHidden: SetAppHiddenUseCase,
    val rename: RenameAppUseCase,
    val addToHome: AddAppToHomeUseCase,
    val addToDock: AddAppToDockUseCase,
    val store: OpenStoreListingUseCase,
    val external: SearchExternallyUseCase,
    val pauseProfile: SetProfilePausedUseCase,
    val updateSettings: UpdateSettingsUseCase,
)

class DrawerViewModel(
    private val observers: DrawerObservers,
    private val actions: DrawerActions,
) : MviViewModel<DrawerState, DrawerIntent, DrawerEffect>(DrawerState()) {

    private var apps: List<App> = emptyList()
    private var badges: Map<PackageUserKey, Int> = emptyMap()
    private var profiles: List<UserProfile> = emptyList()
    private var suggested: List<App> = emptyList()
    private var menuJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                observers.visibleApps(),
                observers.profiles(),
                observers.settings(),
                observers.badges(),
                observers.suggested(SUGGESTION_LIMIT),
            ) { apps, profiles, settings, badges, suggested -> Snapshot(apps, profiles, settings, badges, suggested) }
                .collect(::applySnapshot)
        }
    }

    override fun onIntent(intent: DrawerIntent) {
        when (intent) {
            is DrawerIntent.QueryChanged -> updateQuery(intent.query)
            DrawerIntent.SubmitSearch -> submitSearch()
            is DrawerIntent.SelectProfile -> {
                reduce { copy(selectedProfile = intent.profile) }
                rebuildRows()
            }
            is DrawerIntent.SetWorkPaused -> {
                val serial = profiles.firstOrNull { it.type == ProfileType.Work }?.serial ?: return
                viewModelScope.launch { actions.pauseProfile(serial, intent.paused) }
            }
            is DrawerIntent.Launch -> viewModelScope.launch {
                if (!actions.launch(intent.app, intent.bounds)) emit(DrawerEffect.ShowMessage(DrawerMessage.LaunchFailed))
            }
            is DrawerIntent.ShowMenu -> showMenu(intent.app)
            DrawerIntent.DismissMenu -> dismissMenu()
            is DrawerIntent.MenuAction -> onMenuAction(intent.app, intent.action)
            is DrawerIntent.LaunchShortcut -> {
                dismissMenu()
                if (!actions.launchShortcut(intent.shortcut.key, intent.bounds)) {
                    emit(DrawerEffect.ShowMessage(DrawerMessage.LaunchFailed))
                }
            }
            is DrawerIntent.PinShortcut -> {
                dismissMenu()
                viewModelScope.launch {
                    actions.pinShortcut(intent.shortcut.key, intent.shortcut.shortLabel, 0, intent.shortcut.isPinned)
                    emit(DrawerEffect.ShowMessage(DrawerMessage.AddedToHome))
                }
            }
            is DrawerIntent.Rename -> {
                val rename = currentState.rename
                reduce { copy(rename = null) }
                if (rename != null) viewModelScope.launch { actions.rename(intent.app, intent.label, rename.systemLabel) }
            }
            DrawerIntent.DismissRename -> reduce { copy(rename = null) }
            DrawerIntent.SearchWeb -> actions.external.web(currentState.query)
            DrawerIntent.SearchStore -> actions.external.store(currentState.query)
            DrawerIntent.CopyCalculation -> currentState.calculator?.let {
                emit(DrawerEffect.CopyText(it.result))
                emit(DrawerEffect.ShowMessage(DrawerMessage.Copied))
            }
            is DrawerIntent.ChangeSort -> viewModelScope.launch {
                actions.updateSettings { it.copy(drawer = it.drawer.copy(sort = intent.sort)) }
            }
            DrawerIntent.Closed -> {
                menuJob?.cancel()
                reduce { copy(query = "", results = emptyList(), calculator = null, menu = null) }
            }
        }
    }

    private fun applySnapshot(snapshot: Snapshot) {
        apps = snapshot.apps
        badges = snapshot.badges
        profiles = snapshot.profiles
        suggested = snapshot.suggested
        val available = buildList {
            add(ProfileType.Personal)
            if (snapshot.profiles.any { it.type == ProfileType.Work }) add(ProfileType.Work)
            if (snapshot.profiles.any { it.type == ProfileType.Private }) add(ProfileType.Private)
        }
        reduce {
            copy(
                ready = true,
                appearance = snapshot.settings.appearance,
                drawer = snapshot.settings.drawer,
                profiles = available,
                selectedProfile = selectedProfile.takeIf { it in available } ?: ProfileType.Personal,
                workPaused = snapshot.profiles.firstOrNull { it.type == ProfileType.Work }?.isPaused == true,
                menu = menu?.takeIf { open -> snapshot.apps.any { it.key == open.app } },
            )
        }
        rebuildRows()
        if (currentState.isSearching) updateQuery(currentState.query)
    }

    private fun rebuildRows() {
        val state = currentState
        val profileApps = apps.filter { it.profile == state.selectedProfile }.map(::toDrawerApp)
        val (rows, sections) = buildRows(profileApps, state.drawer.sort)
        val suggestions = if (state.selectedProfile == ProfileType.Personal) suggested.map(::toDrawerApp) else emptyList()
        reduce { copy(rows = rows, sections = sections, suggestions = suggestions) }
    }

    private fun updateQuery(query: String) {
        if (query.isBlank()) {
            reduce { copy(query = query, results = emptyList(), calculator = null) }
            return
        }
        val results = actions.search(apps, query).take(MAX_RESULTS).map(::toDrawerApp)
        reduce { copy(query = query, results = results, calculator = actions.evaluate(query)) }
    }

    private fun submitSearch() {
        val state = currentState
        val first = state.results.firstOrNull()
        when {
            first != null -> onIntent(DrawerIntent.Launch(first.key, null))
            state.calculator != null -> onIntent(DrawerIntent.CopyCalculation)
            state.query.isNotBlank() -> onIntent(DrawerIntent.SearchWeb)
        }
    }

    private fun showMenu(app: AppKey) {
        menuJob?.cancel()
        reduce { copy(menu = DrawerMenu(app)) }
        menuJob = viewModelScope.launch {
            val shortcuts = runCatching { actions.shortcuts(app) }.getOrDefault(emptyList())
            reduce {
                val open = menu
                if (open?.app == app) copy(menu = open.copy(shortcuts = shortcuts, loadingShortcuts = false)) else this
            }
        }
    }

    private fun dismissMenu() {
        menuJob?.cancel()
        reduce { copy(menu = null) }
    }

    private fun onMenuAction(key: AppKey, action: DrawerAppAction) {
        dismissMenu()
        val app = apps.firstOrNull { it.key == key }
        when (action) {
            DrawerAppAction.AppInfo -> actions.appInfo(key, null)
            DrawerAppAction.AddToHome -> viewModelScope.launch {
                actions.addToHome(key, 0)
                emit(DrawerEffect.ShowMessage(DrawerMessage.AddedToHome))
            }
            DrawerAppAction.AddToDock -> viewModelScope.launch {
                val added = actions.addToDock(key)
                emit(DrawerEffect.ShowMessage(if (added) DrawerMessage.AddedToDock else DrawerMessage.DockFull))
            }
            DrawerAppAction.Hide -> viewModelScope.launch {
                actions.setHidden(key, true)
                emit(DrawerEffect.ShowMessage(DrawerMessage.AppHidden))
            }
            DrawerAppAction.Rename -> if (app != null) {
                reduce { copy(rename = DrawerRename(key, app.label, app.systemLabel)) }
            }
            DrawerAppAction.Uninstall -> if (app?.isSystem == true) {
                emit(DrawerEffect.ShowMessage(DrawerMessage.CannotUninstall))
            } else {
                actions.uninstall(key)
            }
            DrawerAppAction.Store -> actions.store(key.packageName)
        }
    }

    private fun toDrawerApp(app: App) = DrawerApp(
        key = app.key,
        label = app.label,
        systemLabel = app.systemLabel,
        profile = app.profile,
        isPaused = app.isSuspended,
        isSystem = app.isSystem,
        badge = badges[app.key.toPackageUserKey()] ?: 0,
        section = sectionOf(app.label),
    )

    private data class Snapshot(
        val apps: List<App>,
        val profiles: List<UserProfile>,
        val settings: LauncherSettings,
        val badges: Map<PackageUserKey, Int>,
        val suggested: List<App>,
    )

    companion object {
        const val SUGGESTION_LIMIT = 8
        private const val MAX_RESULTS = 60

        fun sectionOf(label: String): String {
            val first = AppSearchEngine.normalize(label.trim()).firstOrNull() ?: return "#"
            return when {
                first.isDigit() -> "#"
                first.isLetter() -> first.uppercaseChar().toString()
                else -> "#"
            }
        }

        fun buildRows(apps: List<DrawerApp>, sort: AppSort): Pair<List<DrawerRow>, List<DrawerSection>> {
            if (sort != AppSort.Alphabetical) return apps.map { DrawerRow.Item(it) } to emptyList()
            val rows = mutableListOf<DrawerRow>()
            val sections = mutableListOf<DrawerSection>()
            var current: String? = null
            apps.forEach { app ->
                if (app.section != current) {
                    current = app.section
                    sections += DrawerSection(app.section, rows.size)
                    rows += DrawerRow.Header(app.section)
                }
                rows += DrawerRow.Item(app)
            }
            return rows to sections
        }
    }
}
