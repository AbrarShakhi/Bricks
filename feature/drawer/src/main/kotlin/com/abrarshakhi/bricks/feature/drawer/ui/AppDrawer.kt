package com.abrarshakhi.bricks.feature.drawer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.abrarshakhi.bricks.core.designsystem.component.EmptyState
import com.abrarshakhi.bricks.core.designsystem.component.InkSearchField
import com.abrarshakhi.bricks.core.designsystem.component.SectionHeader
import com.abrarshakhi.bricks.core.designsystem.component.paper
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.domain.model.AppSort
import com.abrarshakhi.bricks.core.domain.model.DrawerLayout
import com.abrarshakhi.bricks.core.domain.model.ProfileType
import com.abrarshakhi.bricks.core.domain.usecase.apps.CalculatorResult
import com.abrarshakhi.bricks.core.ui.component.TextInputDialog
import com.abrarshakhi.bricks.core.ui.drawer.LauncherDrawerState
import com.abrarshakhi.bricks.core.ui.theme.toTextureStyle
import com.abrarshakhi.bricks.feature.drawer.DrawerIntent
import com.abrarshakhi.bricks.feature.drawer.DrawerRow
import com.abrarshakhi.bricks.feature.drawer.DrawerState
import com.abrarshakhi.bricks.feature.drawer.R
import com.abrarshakhi.paperui.PuiButtonDefaults
import com.abrarshakhi.paperui.PuiCard
import com.abrarshakhi.paperui.PuiDragHandle
import com.abrarshakhi.paperui.PuiDropdownMenu
import com.abrarshakhi.paperui.PuiDropdownMenuItem
import com.abrarshakhi.paperui.PuiHorizontalDivider
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiIconButton
import com.abrarshakhi.paperui.PuiListItem
import com.abrarshakhi.paperui.PuiSnackbarHost
import com.abrarshakhi.paperui.PuiSnackbarHostState
import com.abrarshakhi.paperui.PuiSwitch
import com.abrarshakhi.paperui.PuiTab
import com.abrarshakhi.paperui.PuiTabRow
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme
import kotlinx.coroutines.launch
import com.abrarshakhi.bricks.core.ui.R as UiR

@Composable
internal fun AppDrawer(
    state: DrawerState,
    onIntent: (DrawerIntent) -> Unit,
    drawerState: LauncherDrawerState,
    snackbar: PuiSnackbarHostState,
    onOpenSettings: () -> Unit,
    onOpenHiddenApps: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val nestedScroll = remember(drawerState) { drawerState.nestedScrollConnection { velocity -> drawerState.settle(velocity) } }
    val closed by remember(drawerState) { derivedStateOf { drawerState.progress <= 0f } }
    val open by remember(drawerState) { derivedStateOf { drawerState.isOpen } }

    LaunchedEffect(closed) {
        if (closed) {
            onIntent(DrawerIntent.Closed)
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            gridState.scrollToItem(0)
        }
    }
    LaunchedEffect(drawerState.searchRequests) {
        if (drawerState.searchRequests > 0) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }
    LaunchedEffect(open, state.drawer.autoFocusSearch) {
        if (open && state.drawer.autoFocusSearch) runCatching { focusRequester.requestFocus() }
    }
    LaunchedEffect(state.query) {
        if (gridState.firstVisibleItemIndex != 0 || gridState.firstVisibleItemScrollOffset != 0) {
            gridState.scrollToItem(0)
        }
    }
    BackHandler(enabled = !closed) {
        if (state.isSearching) onIntent(DrawerIntent.QueryChanged("")) else scope.launch { drawerState.close() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paper(PuiTheme.colors.paper, PuiTheme.colors.ink, state.appearance.texture.toTextureStyle()),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tablet = min(maxWidth, maxHeight) >= 600.dp
            val iconSize = (if (tablet) 58.dp else 50.dp) * state.appearance.iconScale
            val style = DrawerItemStyle(
                iconSize = iconSize,
                iconStyle = state.appearance.iconStyle,
                showLabels = state.drawer.showLabels,
                list = state.drawer.layout == DrawerLayout.List,
            )
            Column(modifier = Modifier.fillMaxSize()) {
                PuiHorizontalDivider(
                    modifier = Modifier.graphicsLayer { alpha = ((1f - drawerState.progress) * EDGE_FADE).coerceIn(0f, 1f) },
                    thickness = PuiTheme.stroke.heavy,
                )
                DrawerHeader(
                    state = state,
                    onIntent = onIntent,
                    drawerState = drawerState,
                    focusRequester = focusRequester,
                    onOpenSettings = onOpenSettings,
                    onOpenHiddenApps = onOpenHiddenApps,
                )
                if (state.profiles.size > 1 && !state.isSearching) {
                    ProfileTabs(state, onIntent)
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val columns = if (style.list) GridCells.Adaptive(320.dp) else GridCells.Adaptive(maxOf(DrawerItemMinWidth, iconSize + 34.dp))
                    val showIndex = !state.isSearching && !style.list && state.drawer.showIndex &&
                        state.drawer.sort == AppSort.Alphabetical && state.sections.size > 1
                    val navigation = WindowInsets.navigationBars.asPaddingValues()
                    LazyVerticalGrid(
                        columns = columns,
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScroll)
                            .imePadding(),
                        contentPadding = PaddingValues(
                            start = PuiTheme.spacing.md,
                            end = PuiTheme.spacing.md + if (showIndex) SCROLLER_WIDTH else 0.dp,
                            top = PuiTheme.spacing.sm,
                            bottom = navigation.calculateBottomPadding() + PuiTheme.spacing.xl,
                        ),
                    ) {
                        if (state.isSearching) {
                            searchContent(state, style, drawerState, onIntent)
                        } else {
                            browseContent(state, style, drawerState, onIntent, iconSize)
                        }
                    }
                    if (showIndex) {
                        val leading = if (state.suggestions.isNotEmpty() && state.drawer.showSuggestions) 1 else 0
                        FastScroller(
                            sections = state.sections,
                            onSectionSelected = { section ->
                                scope.launch { gridState.scrollToItem(section.rowIndex + leading) }
                            },
                            description = stringResource(R.string.drawer_a11y_index),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(
                                    top = PuiTheme.spacing.lg,
                                    bottom = navigation.calculateBottomPadding() + PuiTheme.spacing.lg,
                                    end = PuiTheme.spacing.xs,
                                ),
                        )
                    }
                }
            }
        }
        PuiSnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        )
        val rename = state.rename
        if (rename != null) {
            TextInputDialog(
                title = stringResource(UiR.string.ui_rename_app_title),
                initialValue = rename.label,
                placeholder = rename.systemLabel,
                onDismiss = { onIntent(DrawerIntent.DismissRename) },
                onConfirm = { onIntent(DrawerIntent.Rename(rename.app, it)) },
                resetLabel = stringResource(UiR.string.ui_rename_reset),
                onReset = { onIntent(DrawerIntent.Rename(rename.app, null)) },
            )
        }
    }
}

@Composable
private fun DrawerHeader(
    state: DrawerState,
    onIntent: (DrawerIntent) -> Unit,
    drawerState: LauncherDrawerState,
    focusRequester: FocusRequester,
    onOpenSettings: () -> Unit,
    onOpenHiddenApps: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .draggable(
                state = drawerState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> drawerState.settle(velocity) },
            )
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PuiDragHandle()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 840.dp)
                .padding(horizontal = PuiTheme.spacing.lg, vertical = PuiTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm),
        ) {
            InkSearchField(
                value = state.query,
                onValueChange = { onIntent(DrawerIntent.QueryChanged(it)) },
                placeholder = stringResource(R.string.drawer_search_hint),
                clearLabel = stringResource(R.string.drawer_search_clear),
                onSearch = { onIntent(DrawerIntent.SubmitSearch) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
            Box {
                PuiIconButton(onClick = { menuOpen = true }) {
                    PuiIcon(BricksIcons.More, contentDescription = stringResource(R.string.drawer_options))
                }
                PuiDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    SortOption(AppSort.Alphabetical, stringResource(R.string.drawer_sort_alphabetical), state.drawer.sort) {
                        menuOpen = false
                        onIntent(DrawerIntent.ChangeSort(it))
                    }
                    SortOption(AppSort.MostUsed, stringResource(R.string.drawer_sort_most_used), state.drawer.sort) {
                        menuOpen = false
                        onIntent(DrawerIntent.ChangeSort(it))
                    }
                    SortOption(AppSort.RecentlyInstalled, stringResource(R.string.drawer_sort_recent), state.drawer.sort) {
                        menuOpen = false
                        onIntent(DrawerIntent.ChangeSort(it))
                    }
                    PuiHorizontalDivider(
                        thickness = PuiTheme.stroke.hairline,
                        color = PuiTheme.colors.surfaceMuted,
                        modifier = Modifier.padding(vertical = PuiTheme.spacing.xs),
                    )
                    PuiDropdownMenuItem(
                        onClick = {
                            menuOpen = false
                            onOpenHiddenApps()
                        },
                        leading = { PuiIcon(BricksIcons.EyeOff, contentDescription = null, size = 22.dp) },
                    ) { PuiText(stringResource(R.string.drawer_hidden_apps)) }
                    PuiDropdownMenuItem(
                        onClick = {
                            menuOpen = false
                            scope.launch { drawerState.snapClosed() }
                            onOpenSettings()
                        },
                        leading = { PuiIcon(BricksIcons.Settings, contentDescription = null, size = 22.dp) },
                    ) { PuiText(stringResource(R.string.drawer_settings)) }
                }
            }
        }
    }
}

@Composable
private fun SortOption(sort: AppSort, label: String, current: AppSort, onSelect: (AppSort) -> Unit) {
    PuiDropdownMenuItem(
        onClick = { onSelect(sort) },
        leading = { PuiIcon(BricksIcons.Sort, contentDescription = null, size = 22.dp) },
        trailing = if (sort == current) {
            { PuiIcon(BricksIcons.Check, contentDescription = null, size = 20.dp, tint = PuiTheme.colors.primary) }
        } else {
            null
        },
    ) { PuiText(label) }
}

@Composable
private fun ProfileTabs(state: DrawerState, onIntent: (DrawerIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PuiTheme.spacing.lg)) {
        PuiTabRow(containerColor = PuiTheme.colors.paper) {
            state.profiles.forEach { profile ->
                PuiTab(
                    selected = profile == state.selectedProfile,
                    onClick = { onIntent(DrawerIntent.SelectProfile(profile)) },
                    icon = {
                        PuiIcon(
                            imageVector = when (profile) {
                                ProfileType.Personal -> BricksIcons.Home
                                ProfileType.Work -> BricksIcons.Work
                                ProfileType.Private -> BricksIcons.Lock
                            },
                            contentDescription = null,
                            size = 18.dp,
                        )
                    },
                ) {
                    PuiText(
                        stringResource(
                            when (profile) {
                                ProfileType.Personal -> R.string.drawer_profile_personal
                                ProfileType.Work -> R.string.drawer_profile_work
                                ProfileType.Private -> R.string.drawer_profile_private
                            },
                        ),
                    )
                }
            }
        }
        if (state.selectedProfile == ProfileType.Work) {
            Spacer(Modifier.height(PuiTheme.spacing.sm))
            PuiListItem(
                headline = { PuiText(stringResource(R.string.drawer_work_apps)) },
                supporting = {
                    PuiText(
                        stringResource(if (state.workPaused) R.string.drawer_work_paused else R.string.drawer_work_active),
                    )
                },
                leading = { PuiIcon(if (state.workPaused) BricksIcons.Pause else BricksIcons.Work, contentDescription = null) },
                trailing = { PuiSwitch(checked = !state.workPaused, onCheckedChange = null) },
                onClick = { onIntent(DrawerIntent.SetWorkPaused(!state.workPaused)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyGridScope.browseContent(
    state: DrawerState,
    style: DrawerItemStyle,
    drawerState: LauncherDrawerState,
    onIntent: (DrawerIntent) -> Unit,
    iconSize: Dp,
) {
    if (state.drawer.showSuggestions && state.suggestions.isNotEmpty()) {
        item(key = "suggestions", span = { GridItemSpan(maxLineSpan) }, contentType = "suggestions") {
            Suggestions(state, style.copy(list = false), drawerState, onIntent, iconSize)
        }
    }
    items(
        items = state.rows,
        key = { it.key },
        span = { row -> if (row is DrawerRow.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
        contentType = { row -> if (row is DrawerRow.Header) "header" else "app" },
    ) { row ->
        when (row) {
            is DrawerRow.Header -> SectionLetter(row.letter)
            is DrawerRow.Item -> DrawerAppItem(row.app, style, state.menu, drawerState, onIntent)
        }
    }
    if (state.ready && state.rows.isEmpty()) {
        item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            EmptyState(
                title = stringResource(R.string.drawer_empty_title),
                message = stringResource(R.string.drawer_empty_message),
                icon = BricksIcons.Apps,
            )
        }
    }
}

@Composable
private fun Suggestions(
    state: DrawerState,
    style: DrawerItemStyle,
    drawerState: LauncherDrawerState,
    onIntent: (DrawerIntent) -> Unit,
    iconSize: Dp,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = PuiTheme.spacing.md)) {
        SectionHeader(title = stringResource(R.string.drawer_suggested))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val count = (maxWidth / maxOf(DrawerItemMinWidth, iconSize + 34.dp)).toInt().coerceAtLeast(1)
            Row(modifier = Modifier.fillMaxWidth()) {
                state.suggestions.take(count).forEach { app ->
                    DrawerAppItem(
                        app = app,
                        style = style,
                        menu = state.menu,
                        drawerState = drawerState,
                        onIntent = onIntent,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(count - state.suggestions.take(count).size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SectionLetter(letter: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PuiTheme.spacing.md, bottom = PuiTheme.spacing.xs, start = PuiTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm),
    ) {
        PuiText(text = letter, style = PuiTheme.typography.headlineSmall, color = PuiTheme.colors.primary)
        PuiHorizontalDivider(
            thickness = PuiTheme.stroke.hairline,
            color = PuiTheme.colors.ink.copy(alpha = 0.35f),
            modifier = Modifier.weight(1f),
        )
    }
}

private fun LazyGridScope.searchContent(
    state: DrawerState,
    style: DrawerItemStyle,
    drawerState: LauncherDrawerState,
    onIntent: (DrawerIntent) -> Unit,
) {
    state.calculator?.let { result ->
        item(key = "calculator", span = { GridItemSpan(maxLineSpan) }) {
            CalculatorCard(result, onCopy = { onIntent(DrawerIntent.CopyCalculation) })
        }
    }
    if (state.results.isEmpty() && state.calculator == null) {
        item(key = "no-results", span = { GridItemSpan(maxLineSpan) }) {
            EmptyState(
                title = stringResource(R.string.drawer_no_results_title),
                message = stringResource(R.string.drawer_no_results_message, state.query),
                icon = BricksIcons.Search,
            )
        }
    } else if (state.results.isNotEmpty()) {
        item(key = "results-header", span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(title = pluralStringResource(R.plurals.drawer_results, state.results.size, state.results.size))
        }
        items(state.results, key = { "result:" + it.key.flattened }, contentType = { "app" }) { app ->
            DrawerAppItem(app, style, state.menu, drawerState, onIntent)
        }
    }
    item(key = "external", span = { GridItemSpan(maxLineSpan) }) {
        ExternalSearch(state.query, onIntent)
    }
}

@Composable
private fun CalculatorCard(result: CalculatorResult, onCopy: () -> Unit) {
    PuiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PuiTheme.spacing.sm),
        color = PuiTheme.colors.accent,
        elevation = PuiTheme.elevation.level3,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                PuiText(
                    text = result.expression,
                    style = PuiTheme.typography.bodyMedium,
                    color = PuiTheme.colors.onAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PuiText(
                    text = "= ${result.result}",
                    style = PuiTheme.typography.displaySmall,
                    color = PuiTheme.colors.onAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PuiIconButton(onClick = onCopy, colors = PuiButtonDefaults.filled(container = PuiTheme.colors.surface, content = PuiTheme.colors.onSurface)) {
                PuiIcon(BricksIcons.Copy, contentDescription = stringResource(R.string.drawer_copy_result))
            }
        }
    }
}

@Composable
private fun ExternalSearch(query: String, onIntent: (DrawerIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PuiTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md),
    ) {
        PuiListItem(
            headline = { PuiText(stringResource(R.string.drawer_search_web, query), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leading = { PuiIcon(BricksIcons.Globe, contentDescription = null) },
            trailing = { PuiIcon(BricksIcons.ChevronRight, contentDescription = null, size = 20.dp) },
            onClick = { onIntent(DrawerIntent.SearchWeb) },
            modifier = Modifier.fillMaxWidth(),
        )
        PuiListItem(
            headline = { PuiText(stringResource(R.string.drawer_search_store, query), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leading = { PuiIcon(BricksIcons.Store, contentDescription = null) },
            trailing = { PuiIcon(BricksIcons.ChevronRight, contentDescription = null, size = 20.dp) },
            onClick = { onIntent(DrawerIntent.SearchStore) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val EDGE_FADE = 6f
