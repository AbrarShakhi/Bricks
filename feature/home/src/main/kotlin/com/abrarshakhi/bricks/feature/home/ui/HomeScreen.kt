package com.abrarshakhi.bricks.feature.home.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.abrarshakhi.bricks.core.designsystem.component.paper
import com.abrarshakhi.bricks.core.domain.layout.HomeLayoutEditor
import com.abrarshakhi.bricks.core.domain.model.Backdrop
import com.abrarshakhi.bricks.core.domain.model.GestureAction
import com.abrarshakhi.bricks.core.domain.model.GridCell
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.usecase.home.DropTarget
import com.abrarshakhi.bricks.core.ui.dragdrop.DragDrop
import com.abrarshakhi.bricks.core.ui.dragdrop.DragDropController
import com.abrarshakhi.bricks.core.ui.dragdrop.DragGhostLayer
import com.abrarshakhi.bricks.core.ui.dragdrop.DragPayload
import com.abrarshakhi.bricks.core.ui.dragdrop.LocalDragDropController
import com.abrarshakhi.bricks.core.ui.dragdrop.containsRoot
import com.abrarshakhi.bricks.core.ui.dragdrop.rootToLocal
import com.abrarshakhi.bricks.core.ui.drawer.LauncherDrawerState
import com.abrarshakhi.bricks.core.ui.theme.isDark
import com.abrarshakhi.bricks.core.ui.theme.toTextureStyle
import com.abrarshakhi.bricks.core.ui.util.CoordinatesHolder
import com.abrarshakhi.bricks.core.ui.util.SystemBarsAppearance
import com.abrarshakhi.bricks.core.ui.util.WallpaperScrollEffect
import com.abrarshakhi.bricks.core.ui.util.rememberWallpaperIsLight
import com.abrarshakhi.bricks.feature.home.HomeGesture
import com.abrarshakhi.bricks.feature.home.HomeIntent
import com.abrarshakhi.bricks.feature.home.HomePage
import com.abrarshakhi.bricks.feature.home.HomeState
import com.abrarshakhi.bricks.feature.home.HomeTile
import com.abrarshakhi.bricks.feature.home.R
import com.abrarshakhi.paperui.PuiSnackbarHost
import com.abrarshakhi.paperui.PuiSnackbarHostState
import com.abrarshakhi.paperui.PuiSurface
import com.abrarshakhi.paperui.PuiTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    drawerState: LauncherDrawerState,
    pagerState: PagerState,
    dragController: DragDropController,
    snackbarHost: PuiSnackbarHostState,
    drawerContent: @Composable () -> Unit,
) {
    val resolver = remember { HomeDropResolver() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val latestState by rememberUpdatedState(state)
    val onWallpaper = state.appearance.backdrop == Backdrop.Wallpaper
    val folderTitle = stringResource(R.string.home_folder_default)
    var homeMenuAnchor by remember { mutableStateOf(Offset.Zero) }
    var dockVertical by remember { mutableStateOf(false) }
    val session = dragController.session
    val dragging = session != null

    val hover by remember(resolver, dragController) {
        derivedStateOf {
            val active = dragController.session ?: return@derivedStateOf HomeDropTarget.None
            resolver.resolve(
                rootPosition = dragController.pointer,
                payload = active.payload,
                grabOffset = active.grabOffset,
                state = latestState,
                page = pagerState.currentPage,
                dockVertical = dockVertical,
            )
        }
    }

    DisposableEffect(dragController, resolver) {
        dragController.dropHandler = { drop ->
            handleDrop(drop, resolver, latestState, pagerState.currentPage, dockVertical, folderTitle, onIntent)
        }
        onDispose { dragController.dropHandler = null }
    }

    LaunchedEffect(dragging) {
        if (!dragging) return@LaunchedEffect
        val edge = with(density) { EDGE_ZONE.toPx() }
        while (true) {
            delay(EDGE_DWELL_MILLIS)
            val pager = resolver.pager ?: continue
            val pointer = dragController.pointer
            if (!pager.containsRoot(pointer)) continue
            val local = pager.rootToLocal(pointer)
            val current = pagerState.currentPage
            when {
                local.x < edge && current > 0 -> pagerState.animateScrollToPage(current - 1)
                local.x > pager.size.width - edge && current < pagerState.pageCount - 1 ->
                    pagerState.animateScrollToPage(current + 1)
            }
        }
    }

    WallpaperScrollEffect(
        pagerState = pagerState,
        pageCount = state.pageCount,
        enabled = onWallpaper && state.home.wallpaperScrolling,
    )

    val drawerOpen by remember { derivedStateOf { drawerState.progress > 0.5f } }
    val wallpaperLight = rememberWallpaperIsLight()
    val darkTheme = state.appearance.themeMode.isDark()
    SystemBarsAppearance(
        lightContent = when {
            drawerOpen -> darkTheme
            onWallpaper -> !wallpaperLight
            else -> darkTheme
        },
    )

    BackHandler {
        when {
            drawerState.isVisible -> scope.launch { drawerState.close() }
            state.menu != null || state.homeMenuVisible -> onIntent(HomeIntent.DismissMenus)
            state.openFolderId != null -> onIntent(HomeIntent.CloseFolder)
            state.editMode -> onIntent(HomeIntent.ExitEditMode)
            pagerState.currentPage != 0 -> scope.launch { pagerState.animateScrollToPage(0) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (onWallpaper) {
                    Modifier.background(Color.Black.copy(alpha = state.appearance.wallpaperDim))
                } else {
                    Modifier.paper(PuiTheme.colors.paper, PuiTheme.colors.ink, state.appearance.texture.toTextureStyle())
                },
            ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screen = ScreenSpec(maxWidth, maxHeight)
            dockVertical = screen.isLandscapePhone
            HomeContent(
                state = state,
                screen = screen,
                onIntent = onIntent,
                drawerState = drawerState,
                pagerState = pagerState,
                resolver = resolver,
                hover = hover,
                dragging = dragging,
                onWallpaper = onWallpaper,
                onHomeLongPress = { anchor ->
                    homeMenuAnchor = anchor
                    onIntent(HomeIntent.ShowHomeMenu)
                },
            )
            DropZones(
                visible = dragging,
                showUninstall = when (val payload = session?.payload) {
                    is DragPayload.DrawerApp -> true
                    is DragPayload.HomeItem -> (state.findTile(payload.itemId) as? HomeTile.App)?.isSystem == false
                    null -> false
                },
                hover = hover,
                resolver = resolver,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .widthIn(max = 640.dp),
            )
            FolderOverlay(
                folder = state.openFolder,
                environment = tileEnvironment(state, onWallpaper = false, iconSize = min(maxWidth / 6, 64.dp), page = pagerState.currentPage),
                menu = state.menu,
                maxHeight = maxHeight,
                onIntent = onIntent,
            )
        }

        DrawerLayer(drawerState = drawerState, content = drawerContent)

        HomeOptionsMenu(visible = state.homeMenuVisible, anchor = homeMenuAnchor, onIntent = onIntent)
        HomeDialogs(state = state, onIntent = onIntent)
        DragGhostLayer(controller = dragController, modifier = Modifier.fillMaxSize())
        PuiSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeState,
    screen: ScreenSpec,
    onIntent: (HomeIntent) -> Unit,
    drawerState: LauncherDrawerState,
    pagerState: PagerState,
    resolver: HomeDropResolver,
    hover: HomeDropTarget,
    dragging: Boolean,
    onWallpaper: Boolean,
    onHomeLongPress: (Offset) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val downDistance = remember { FloatHolder() }
    val swipeUpEnabled = state.gestures.swipeUp != GestureAction.None
    val threshold = with(LocalDensity.current) { SWIPE_DOWN_THRESHOLD.toPx() }
    val homeDrag = rememberDraggableState { delta ->
        if (swipeUpEnabled && (delta < 0f || drawerState.progress > 0f)) {
            drawerState.consume(delta)
        } else if (drawerState.progress == 0f) {
            downDistance.value += delta
        }
    }
    val pageCount = state.pageCount
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val progress = drawerState.progress
                alpha = 1f - 0.9f * progress
                val scale = 1f - 0.05f * progress
                scaleX = scale
                scaleY = scale
            }
            .draggable(
                state = homeDrag,
                orientation = Orientation.Vertical,
                enabled = !dragging && state.openFolderId == null && !state.editMode,
                onDragStarted = {
                    downDistance.value = 0f
                    drawerState.interrupt()
                },
                onDragStopped = { velocity ->
                    val pulledDown = downDistance.value > threshold || (velocity > FLING_DOWN_VELOCITY && downDistance.value > 0f)
                    if (drawerState.progress == 0f && pulledDown) {
                        onIntent(HomeIntent.Gesture(HomeGesture.SwipeDown))
                    } else {
                        if (state.gestures.swipeUp == GestureAction.OpenSearch && velocity < 0f) {
                            drawerState.requestOpen(focusSearch = true)
                        }
                        scope.launch { drawerState.settle(velocity) }
                    }
                    downDistance.value = 0f
                },
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val contentModifier = Modifier
            .fillMaxHeight()
            .widthIn(max = screen.maxContentWidth)
            .padding(horizontal = screen.sidePadding)
        if (screen.isLandscapePhone) {
            Row(modifier = contentModifier) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    HomeHeader(state, onIntent, onWallpaper, compact = true)
                    PagerArea(state, pagerState, pageCount, resolver, hover, dragging, onWallpaper, onIntent, onHomeLongPress, Modifier.weight(1f))
                    PageIndicator(pagerState, pageCount, Modifier.fillMaxWidth())
                }
                if (state.home.dockEnabled) {
                    Spacer(Modifier.width(PuiTheme.spacing.sm))
                    val dockIcon = dockIconSize(screen.height - 48.dp, state.home.dockSlots, state.appearance.iconScale, 52.dp)
                    Dock(
                        state = state,
                        vertical = true,
                        iconSize = dockIcon,
                        hover = hover,
                        resolver = resolver,
                        onIntent = onIntent,
                        modifier = Modifier.fillMaxHeight().padding(vertical = PuiTheme.spacing.sm),
                    )
                }
            }
        } else {
            Column(modifier = contentModifier) {
                HomeHeader(state, onIntent, onWallpaper, compact = false)
                PagerArea(state, pagerState, pageCount, resolver, hover, dragging, onWallpaper, onIntent, onHomeLongPress, Modifier.weight(1f))
                PageIndicator(pagerState, pageCount, Modifier.fillMaxWidth())
                if (state.editMode) {
                    EditPageActions(
                        page = pagerState.currentPage,
                        canRemove = state.pageCount > 1 && pagerState.currentPage < state.pageCount,
                        canAdd = state.pageCount < HomeLayoutEditor.MAX_PAGES,
                        onIntent = onIntent,
                        modifier = Modifier.padding(vertical = PuiTheme.spacing.sm),
                    )
                } else if (state.home.showSearchBar) {
                    SearchPill(
                        onClick = {
                            drawerState.requestOpen(focusSearch = true)
                            scope.launch { drawerState.open() }
                        },
                        modifier = Modifier
                            .padding(horizontal = PuiTheme.spacing.xs, vertical = PuiTheme.spacing.sm)
                            .widthIn(max = 560.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                }
                if (state.home.dockEnabled) {
                    val available = min(screen.maxContentWidth, screen.width) - screen.sidePadding * 2
                    val dockIcon = dockIconSize(available, state.home.dockSlots, state.appearance.iconScale, if (screen.isTablet) 64.dp else 58.dp)
                    Dock(
                        state = state,
                        vertical = false,
                        iconSize = dockIcon,
                        hover = hover,
                        resolver = resolver,
                        onIntent = onIntent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = PuiTheme.spacing.sm),
                    )
                } else {
                    Spacer(Modifier.height(PuiTheme.spacing.md))
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(state: HomeState, onIntent: (HomeIntent) -> Unit, onWallpaper: Boolean, compact: Boolean) {
    when {
        state.editMode -> EditModeBar(onIntent = onIntent, modifier = Modifier.padding(vertical = PuiTheme.spacing.sm))
        state.home.showGlance -> GlanceHeader(
            glance = state.glance,
            onWallpaper = onWallpaper,
            compact = compact,
            onIntent = onIntent,
            modifier = Modifier.padding(
                start = PuiTheme.spacing.sm,
                end = PuiTheme.spacing.sm,
                top = PuiTheme.spacing.md,
                bottom = PuiTheme.spacing.sm,
            ),
        )
        else -> Spacer(Modifier.height(PuiTheme.spacing.md))
    }
}

@Composable
private fun PagerArea(
    state: HomeState,
    pagerState: PagerState,
    pageCount: Int,
    resolver: HomeDropResolver,
    hover: HomeDropTarget,
    dragging: Boolean,
    onWallpaper: Boolean,
    onIntent: (HomeIntent) -> Unit,
    onHomeLongPress: (Offset) -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        PagerContent(state, pagerState, resolver, hover, dragging, onWallpaper, onIntent, onHomeLongPress)
        if (state.showDefaultPrompt && !state.editMode && !dragging) {
            DefaultLauncherPrompt(
                onIntent = onIntent,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 560.dp)
                    .padding(horizontal = PuiTheme.spacing.xs, vertical = PuiTheme.spacing.sm),
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.PagerContent(
    state: HomeState,
    pagerState: PagerState,
    resolver: HomeDropResolver,
    hover: HomeDropTarget,
    dragging: Boolean,
    onWallpaper: Boolean,
    onIntent: (HomeIntent) -> Unit,
    onHomeLongPress: (Offset) -> Unit,
) {
    val spec = gridSpec(
        width = maxWidth,
        height = maxHeight,
        grid = state.grid,
        iconScale = state.appearance.iconScale,
        labelsEnabled = state.home.showLabels,
    )
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    LaunchedEffect(spec.cellWidth, spec.cellHeight, landscape) {
        onIntent(HomeIntent.CellSizeChanged(spec.cellWidth.value, spec.cellHeight.value, landscape))
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { resolver.pager = it },
        beyondViewportPageCount = 1,
        key = { it },
    ) { pageIndex ->
        val page = state.pages.getOrNull(pageIndex)
        HomePageGrid(
            page = page,
            pageIndex = pageIndex,
            spec = spec,
            environment = tileEnvironment(state, onWallpaper, spec.iconSize, pageIndex).copy(showLabel = spec.showLabels),
            state = state,
            hover = hover,
            dragging = dragging,
            onIntent = onIntent,
            onHomeLongPress = onHomeLongPress,
        )
    }
}

@Composable
private fun HomePageGrid(
    page: HomePage?,
    pageIndex: Int,
    spec: GridSpec,
    environment: TileEnvironment,
    state: HomeState,
    hover: HomeDropTarget,
    dragging: Boolean,
    onIntent: (HomeIntent) -> Unit,
    onHomeLongPress: (Offset) -> Unit,
) {
    val holder = remember { CoordinatesHolder() }
    val doubleTapEnabled = state.gestures.doubleTap != GestureAction.None
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (state.editMode || (dragging && page == null)) {
                    Modifier
                        .padding(PuiTheme.spacing.xs)
                        .border(PuiTheme.stroke.regular, PuiTheme.colors.ink.copy(alpha = 0.45f), PuiTheme.shapes.large)
                        .background(PuiTheme.colors.surface.copy(alpha = 0.12f), PuiTheme.shapes.large)
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .onGloballyPositioned { holder.coordinates = it }
                .pointerInput(doubleTapEnabled) {
                    detectTapGestures(
                        onDoubleTap = if (doubleTapEnabled) {
                            { onIntent(HomeIntent.Gesture(HomeGesture.DoubleTap)) }
                        } else {
                            null
                        },
                        onLongPress = { offset ->
                            val root = holder.coordinates?.takeIf { it.isAttached }?.localToRoot(offset) ?: offset
                            onHomeLongPress(root)
                        },
                    )
                },
        )
        CellLayout(columns = spec.columns, rows = spec.rows, modifier = Modifier.fillMaxSize()) {
            val cellHover = hover as? HomeDropTarget.Cell
            if (cellHover != null && cellHover.page == pageIndex) {
                Box(
                    modifier = Modifier
                        .cell(cellHover.position)
                        .padding(PuiTheme.spacing.xs)
                        .background(PuiTheme.colors.accent.copy(alpha = 0.35f), PuiTheme.shapes.large)
                        .border(PuiTheme.stroke.bold, PuiTheme.colors.ink, PuiTheme.shapes.large),
                )
            }
            page?.tiles?.forEach { tile ->
                val placement = tile.placement as? Placement.Desktop
                if (placement != null) {
                    key(tile.id) {
                        val merging = hover is HomeDropTarget.Merge && hover.tileId == tile.id
                        val scale by animateFloatAsState(
                            targetValue = if (merging) 1.15f else 1f,
                            animationSpec = PuiTheme.motion.spec(PuiTheme.motion.quick),
                            label = "MergeScale",
                        )
                        HomeTileView(
                            tile = tile,
                            environment = environment,
                            menu = state.menu,
                            onIntent = onIntent,
                            modifier = Modifier
                                .cell(CellPosition(placement.cell.x, placement.cell.y, placement.span.width, placement.span.height))
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Dock(
    state: HomeState,
    vertical: Boolean,
    iconSize: Dp,
    hover: HomeDropTarget,
    resolver: HomeDropResolver,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier,
) {
    val environment = tileEnvironment(state, onWallpaper = false, iconSize = iconSize, page = 0).copy(showLabel = false)
    val cross = iconSize + DOCK_PADDING * 2
    PuiSurface(
        modifier = modifier,
        shape = PuiTheme.shapes.large,
        elevation = PuiTheme.elevation.level3,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(PuiTheme.spacing.xs)
                .then(if (vertical) Modifier.width(cross).fillMaxHeight() else Modifier.fillMaxWidth().height(cross))
                .onGloballyPositioned { resolver.dock = it },
        ) {
            val slots = state.home.dockSlots.coerceAtLeast(1)
            val length = if (vertical) maxHeight else maxWidth
            val slot = length / slots
            val tiles = state.dock
            val content: @Composable () -> Unit = {
                tiles.forEach { tile ->
                    key(tile.id) {
                        val merging = hover is HomeDropTarget.Merge && hover.tileId == tile.id
                        val scale by animateFloatAsState(
                            targetValue = if (merging) 1.15f else 1f,
                            animationSpec = PuiTheme.motion.spec(PuiTheme.motion.quick),
                            label = "DockMergeScale",
                        )
                        HomeTileView(
                            tile = tile,
                            environment = environment,
                            menu = state.menu,
                            onIntent = onIntent,
                            modifier = (if (vertical) Modifier.height(slot).fillMaxWidth() else Modifier.width(slot).fillMaxHeight())
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                        )
                    }
                }
            }
            if (vertical) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { content() }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) { content() }
            }
            val insertion = hover as? HomeDropTarget.Dock
            if (insertion != null) {
                val draggedId = (LocalDragDropController.current.session?.payload as? DragPayload.HomeItem)?.itemId
                val draggedIndex = tiles.indexOfFirst { tile -> tile.id == draggedId }
                val boundary = if (draggedIndex >= 0 && insertion.index >= draggedIndex) insertion.index + 1 else insertion.index
                val start = (length - slot * tiles.size) / 2 + slot * boundary
                Box(
                    modifier = if (vertical) {
                        Modifier
                            .offset(y = start - MARKER_THICKNESS / 2)
                            .fillMaxWidth()
                            .height(MARKER_THICKNESS)
                    } else {
                        Modifier
                            .offset(x = start - MARKER_THICKNESS / 2)
                            .fillMaxHeight()
                            .width(MARKER_THICKNESS)
                    }.background(PuiTheme.colors.primary, PuiTheme.shapes.pill),
                )
            }
        }
    }
}

@Composable
private fun DrawerLayer(drawerState: LauncherDrawerState, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { drawerState.heightPx = it.height.toFloat() }
            .graphicsLayer {
                val progress = drawerState.progress
                translationY = (1f - progress) * size.height
                alpha = if (progress <= 0f) 0f else 1f
            },
    ) {
        content()
    }
}

@Composable
private fun tileEnvironment(state: HomeState, onWallpaper: Boolean, iconSize: Dp, page: Int): TileEnvironment =
    TileEnvironment(
        iconSize = iconSize,
        showLabel = state.home.showLabels,
        iconStyle = state.appearance.iconStyle,
        onWallpaper = onWallpaper,
        editMode = state.editMode,
        dragEnabled = !state.home.layoutLocked,
        page = page,
    )

private fun handleDrop(
    drop: DragDrop,
    resolver: HomeDropResolver,
    state: HomeState,
    page: Int,
    dockVertical: Boolean,
    folderTitle: String,
    onIntent: (HomeIntent) -> Unit,
) {
    val target = resolver.resolve(drop.rootPosition, drop.payload, drop.grabOffset, state, page, dockVertical)
    val domainTarget = when (target) {
        is HomeDropTarget.Cell -> DropTarget.Desktop(target.page, GridCell(target.position.x, target.position.y))
        is HomeDropTarget.Dock -> DropTarget.Dock(target.index)
        is HomeDropTarget.Merge -> DropTarget.Folder(target.tileId, folderTitle)
        HomeDropTarget.Remove, HomeDropTarget.Uninstall, HomeDropTarget.None -> null
    }
    when (val payload = drop.payload) {
        is DragPayload.HomeItem -> when (target) {
            HomeDropTarget.Remove -> onIntent(HomeIntent.RemoveTile(payload.itemId))
            HomeDropTarget.Uninstall -> onIntent(HomeIntent.UninstallTile(payload.itemId))
            else -> domainTarget?.let { onIntent(HomeIntent.MoveTile(payload.itemId, it)) }
        }
        is DragPayload.DrawerApp -> when (target) {
            HomeDropTarget.Uninstall -> onIntent(HomeIntent.UninstallApp(payload.app))
            else -> domainTarget?.let { onIntent(HomeIntent.DropApp(payload.app, it)) }
        }
    }
}

private class FloatHolder {
    var value: Float = 0f
}

private val EDGE_ZONE = 36.dp
private const val EDGE_DWELL_MILLIS = 600L
private val SWIPE_DOWN_THRESHOLD = 56.dp
private const val FLING_DOWN_VELOCITY = 1500f
private val DOCK_PADDING = 10.dp
private val MARKER_THICKNESS = 4.dp
