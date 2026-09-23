package com.abrarshakhi.bricks.feature.widgets.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.component.EmptyState
import com.abrarshakhi.bricks.core.designsystem.component.InkSearchField
import com.abrarshakhi.bricks.core.designsystem.component.ScreenBodyInsets
import com.abrarshakhi.bricks.core.designsystem.component.ScreenTopBar
import com.abrarshakhi.bricks.core.designsystem.component.halftone
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.domain.model.ProfileType
import com.abrarshakhi.bricks.core.domain.model.WidgetProvider
import com.abrarshakhi.bricks.core.domain.usecase.system.WidgetProviderGroup
import com.abrarshakhi.bricks.core.ui.widget.rememberWidgetPreview
import com.abrarshakhi.bricks.feature.widgets.R
import com.abrarshakhi.bricks.feature.widgets.WidgetsIntent
import com.abrarshakhi.bricks.feature.widgets.WidgetsState
import com.abrarshakhi.paperui.PuiCard
import com.abrarshakhi.paperui.PuiCircularProgressIndicator
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiScaffold
import com.abrarshakhi.paperui.PuiScrim
import com.abrarshakhi.paperui.PuiSnackbarHost
import com.abrarshakhi.paperui.PuiSnackbarHostState
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme

@Composable
internal fun WidgetPickerScreen(
    state: WidgetsState,
    onIntent: (WidgetsIntent) -> Unit,
    onBack: () -> Unit,
    cellWidthDp: Float,
    cellHeightDp: Float,
    snackbar: PuiSnackbarHostState,
) {
    PuiScaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = ScreenBodyInsets,
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.widgets_title),
                onBack = onBack,
                backContentDescription = stringResource(R.string.widgets_back),
            )
        },
        snackbarHost = { PuiSnackbarHost(snackbar) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(PuiTheme.colors.paper)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + PuiTheme.spacing.md,
                    bottom = padding.calculateBottomPadding() + PuiTheme.spacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm),
            ) {
                item(key = "search") {
                    InkSearchField(
                        value = state.query,
                        onValueChange = { onIntent(WidgetsIntent.QueryChanged(it)) },
                        placeholder = stringResource(R.string.widgets_search_hint),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PuiTheme.spacing.lg),
                    )
                }
                item(key = "hint") {
                    PuiText(
                        text = stringResource(R.string.widgets_hint),
                        style = PuiTheme.typography.bodySmall,
                        color = PuiTheme.colors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = PuiTheme.spacing.lg, vertical = PuiTheme.spacing.xs),
                    )
                }
                if (!state.loading && state.groups.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            title = stringResource(R.string.widgets_empty_title),
                            message = stringResource(R.string.widgets_empty_message),
                            icon = BricksIcons.Widgets,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                state.groups.forEach { group ->
                    item(key = "header:${group.packageName}:${group.providers.first().key.userSerial}") {
                        GroupHeader(group)
                    }
                    item(key = "row:${group.packageName}:${group.providers.first().key.userSerial}") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = PuiTheme.spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md),
                        ) {
                            items(group.providers, key = { it.key.className }) { provider ->
                                WidgetCard(
                                    provider = provider,
                                    spanLabel = provider.defaultSpan(cellWidthDp, cellHeightDp, state.grid).let { span ->
                                        stringResource(R.string.widgets_span, span.width, span.height)
                                    },
                                    enabled = !state.adding,
                                    onClick = { onIntent(WidgetsIntent.Add(provider, cellWidthDp, cellHeightDp)) },
                                )
                            }
                        }
                    }
                }
            }
            if (state.adding) {
                PuiScrim(visible = true, onDismiss = null)
                PuiCircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun GroupHeader(group: WidgetProviderGroup) {
    val first = group.providers.first()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PuiTheme.spacing.lg)
            .padding(top = PuiTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md),
    ) {
        val icon = rememberWidgetPreview(first.key, 32.dp, icon = true)
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(32.dp))
            } else {
                PuiIcon(BricksIcons.Widgets, contentDescription = null)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            PuiText(
                text = group.appLabel,
                style = PuiTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PuiText(
                text = pluralStringResource(R.plurals.widgets_count, group.providers.size, group.providers.size),
                style = PuiTheme.typography.labelSmall,
                color = PuiTheme.colors.onSurfaceMuted,
            )
        }
        if (first.profile == ProfileType.Work) {
            PuiIcon(BricksIcons.Work, contentDescription = stringResource(R.string.widgets_work), size = 20.dp)
        }
    }
}

@Composable
private fun WidgetCard(
    provider: WidgetProvider,
    spanLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PuiCard(
        modifier = Modifier.width(CARD_WIDTH),
        onClick = onClick,
        enabled = enabled,
        elevation = PuiTheme.elevation.level2,
        contentPadding = PaddingValues(PuiTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PREVIEW_HEIGHT)
                .background(PuiTheme.colors.surfaceMuted, PuiTheme.shapes.small)
                .halftone(color = PuiTheme.colors.ink, dotRadius = 1.dp, spacing = 7.dp, alpha = 0.1f),
            contentAlignment = Alignment.Center,
        ) {
            val preview = rememberWidgetPreview(provider.key, CARD_WIDTH)
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PuiTheme.spacing.sm),
                )
            } else {
                PuiIcon(BricksIcons.Widgets, contentDescription = null, size = 36.dp)
            }
        }
        Spacer(Modifier.height(PuiTheme.spacing.sm))
        PuiText(
            text = provider.label,
            style = PuiTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PuiText(text = spanLabel, style = PuiTheme.typography.labelMedium, color = PuiTheme.colors.primary)
        provider.description?.takeIf { it.isNotBlank() }?.let { description ->
            PuiText(
                text = description,
                style = PuiTheme.typography.bodySmall,
                color = PuiTheme.colors.onSurfaceMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = CARD_WIDTH),
            )
        }
    }
}

private val CARD_WIDTH = 176.dp
private val PREVIEW_HEIGHT = 112.dp
