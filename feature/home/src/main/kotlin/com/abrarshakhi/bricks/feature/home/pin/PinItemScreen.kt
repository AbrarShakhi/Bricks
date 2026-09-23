package com.abrarshakhi.bricks.feature.home.pin

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.bricks.core.designsystem.component.halftone
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.domain.model.IconStyle
import com.abrarshakhi.bricks.core.ui.component.AppIconTile
import com.abrarshakhi.bricks.core.ui.icon.AppIconLoader
import com.abrarshakhi.bricks.core.ui.icon.LoadedIcon
import com.abrarshakhi.bricks.core.ui.mvi.CollectEffects
import com.abrarshakhi.bricks.core.ui.theme.LauncherTheme
import com.abrarshakhi.bricks.core.ui.util.CellSize
import com.abrarshakhi.bricks.core.ui.util.HomeGridMetrics
import com.abrarshakhi.bricks.core.ui.widget.rememberWidgetPreview
import com.abrarshakhi.bricks.feature.home.R
import com.abrarshakhi.paperui.PuiButton
import com.abrarshakhi.paperui.PuiDialog
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTextButton
import com.abrarshakhi.paperui.PuiTheme
import org.koin.compose.koinInject
import kotlin.math.roundToInt
import com.abrarshakhi.bricks.core.ui.R as UiR

@Composable
internal fun PinItemRoute(
    onFinished: (PinOutcome) -> Unit,
    viewModel: PinItemViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val gridMetrics = koinInject<HomeGridMetrics>()
    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.onIntent(PinItemIntent.BindResult(result.resultCode == Activity.RESULT_OK))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is PinItemEffect.RequestBind -> runCatching { bindLauncher.launch(effect.intent) }
                .onFailure { viewModel.onIntent(PinItemIntent.BindResult(false)) }
            is PinItemEffect.Finish -> onFinished(effect.outcome)
        }
    }

    val appearance = state.appearance
    val candidate = state.candidate
    if (appearance != null && candidate != null) {
        LauncherTheme(appearance = appearance) {
            PinItemDialog(
                state = state,
                candidate = candidate,
                cell = gridMetrics.cellSize(state.grid, configuration.screenWidthDp, configuration.screenHeightDp),
                iconStyle = appearance.iconStyle,
                onIntent = viewModel::onIntent,
            )
        }
    }
}

@Composable
private fun PinItemDialog(
    state: PinItemState,
    candidate: PinCandidate,
    cell: CellSize,
    iconStyle: IconStyle,
    onIntent: (PinItemIntent) -> Unit,
) {
    PuiDialog(
        onDismissRequest = { onIntent(PinItemIntent.Dismiss) },
        icon = { PuiIcon(BricksIcons.Home, contentDescription = null) },
        title = { PuiText(stringResource(R.string.home_pin_title)) },
        text = {
            when (candidate) {
                is PinCandidate.Shortcut -> ShortcutPreview(candidate, iconStyle)
                is PinCandidate.Widget -> WidgetPreview(candidate, state, cell)
            }
        },
        dismissButton = {
            PuiTextButton(onClick = { onIntent(PinItemIntent.Dismiss) }) {
                PuiText(stringResource(UiR.string.ui_action_cancel))
            }
        },
        confirmButton = {
            PuiButton(
                onClick = { onIntent(PinItemIntent.Confirm(cell.widthDp, cell.heightDp)) },
                enabled = state.canConfirm,
            ) { PuiText(stringResource(R.string.home_pin_add)) }
        },
    )
}

@Composable
private fun ShortcutPreview(candidate: PinCandidate.Shortcut, iconStyle: IconStyle) {
    val icon = rememberPinIcon(candidate.icon, PREVIEW_ICON_SIZE, iconStyle)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md),
    ) {
        AppIconTile(icon = icon, style = iconStyle, size = PREVIEW_ICON_SIZE)
        PreviewLabels(title = candidate.label, subtitle = candidate.appLabel, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun WidgetPreview(candidate: PinCandidate.Widget, state: PinItemState, cell: CellSize) {
    val preview = rememberWidgetPreview(candidate.provider, WIDGET_PREVIEW_SIZE)
    val span = state.provider?.defaultSpan(cell.widthDp, cell.heightDp, state.grid)
    Column(verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WIDGET_PREVIEW_HEIGHT)
                .background(PuiTheme.colors.surfaceMuted, PuiTheme.shapes.small)
                .halftone(color = PuiTheme.colors.ink, dotRadius = 1.dp, spacing = 7.dp, alpha = 0.1f)
                .border(PuiTheme.stroke.regular, PuiTheme.colors.ink, PuiTheme.shapes.small)
                .padding(PuiTheme.spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            if (preview != null) {
                Image(bitmap = preview, contentDescription = null, contentScale = ContentScale.Fit)
            } else {
                PuiIcon(BricksIcons.Widgets, contentDescription = null, size = 40.dp)
            }
        }
        PreviewLabels(
            title = candidate.label,
            subtitle = if (span != null) {
                stringResource(R.string.home_pin_widget_subtitle, candidate.appLabel, span.width, span.height)
            } else {
                candidate.appLabel
            },
        )
    }
}

@Composable
private fun PreviewLabels(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        PuiText(
            text = title,
            style = PuiTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        PuiText(
            text = subtitle,
            style = PuiTheme.typography.bodySmall,
            color = PuiTheme.colors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun rememberPinIcon(source: PinIconSource, size: Dp, style: IconStyle): LoadedIcon? {
    val loader = koinInject<AppIconLoader>()
    val sizePx = with(LocalDensity.current) { size.toPx().roundToInt() }
    val icon by produceState<LoadedIcon?>(null, source, sizePx, style) {
        value = loader.loadExternal(sizePx, style) { densityDpi -> source.load(densityDpi) }
    }
    return icon
}

private val PREVIEW_ICON_SIZE = 56.dp
private val WIDGET_PREVIEW_SIZE = 240.dp
private val WIDGET_PREVIEW_HEIGHT = 150.dp
