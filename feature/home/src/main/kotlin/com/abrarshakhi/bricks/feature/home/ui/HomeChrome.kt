package com.abrarshakhi.bricks.feature.home.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.component.ComicCaption
import com.abrarshakhi.bricks.core.designsystem.component.rememberWallpaperTextStyle
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.feature.home.GlanceUi
import com.abrarshakhi.bricks.feature.home.HomeIntent
import com.abrarshakhi.bricks.feature.home.R
import com.abrarshakhi.paperui.PuiButton
import com.abrarshakhi.paperui.PuiCard
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiSurface
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTextButton
import com.abrarshakhi.paperui.PuiTheme
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GlanceHeader(
    glance: GlanceUi?,
    onWallpaper: Boolean,
    compact: Boolean,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeMillis = glance?.timeMillis ?: System.currentTimeMillis()
    val parts = rememberTimeParts(timeMillis)
    val clockBase = if (compact) PuiTheme.typography.displaySmall else PuiTheme.typography.displayLarge
    val clockStyle = letteringStyle(clockBase, onWallpaper, if (compact) 2.dp else 3.dp)
    val meridiemStyle = letteringStyle(PuiTheme.typography.headlineSmall, onWallpaper, 2.dp)
    val clockLabel = stringResource(R.string.home_a11y_open_clock)
    val clock: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = clockLabel,
                ) { onIntent(HomeIntent.OpenClock) }
                .semantics(mergeDescendants = true) { contentDescription = parts.spoken },
        ) {
            PuiText(text = parts.time, style = clockStyle)
            if (parts.meridiem != null) {
                PuiText(
                    text = parts.meridiem,
                    style = meridiemStyle,
                    modifier = Modifier.padding(start = 6.dp, bottom = if (compact) 4.dp else 10.dp),
                )
            }
        }
    }
    val details: @Composable () -> Unit = {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm),
        ) {
            ComicCaption(
                text = parts.date,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                ) { onIntent(HomeIntent.OpenCalendar) },
            )
            glance?.batteryLevel?.let { level ->
                InfoChip(
                    icon = if (glance.charging) BricksIcons.Bolt else BricksIcons.Battery,
                    text = stringResource(R.string.home_glance_battery, level),
                    onClick = { onIntent(HomeIntent.OpenBattery) },
                )
            }
            glance?.nextAlarmMillis?.let { alarm ->
                InfoChip(
                    icon = BricksIcons.Alarm,
                    text = rememberAlarmLabel(alarm),
                    onClick = { onIntent(HomeIntent.OpenClock) },
                )
            }
        }
    }
    if (compact) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.lg),
        ) {
            clock()
            details()
        }
    } else {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.xs)) {
            clock()
            details()
        }
    }
}

@Composable
private fun letteringStyle(base: TextStyle, onWallpaper: Boolean, offset: Dp): TextStyle {
    if (onWallpaper) return rememberWallpaperTextStyle(base, shadowOffset = offset)
    val density = LocalDensity.current
    val shadowColor = PuiTheme.colors.primary
    val textColor = PuiTheme.colors.onPaper
    return remember(base, shadowColor, textColor, offset, density) {
        val px = with(density) { offset.toPx() }
        base.copy(color = textColor, shadow = Shadow(shadowColor, Offset(px, px), 0f))
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    PuiSurface(
        shape = PuiTheme.shapes.small,
        color = PuiTheme.colors.surface,
        elevation = PuiTheme.elevation.level1,
        onClick = onClick,
        role = Role.Button,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PuiTheme.spacing.sm, vertical = PuiTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.xs),
        ) {
            PuiIcon(icon, contentDescription = null, size = 16.dp)
            PuiText(text = text, style = PuiTheme.typography.labelMedium)
        }
    }
}

private data class TimeParts(
    val time: String,
    val meridiem: String?,
    val date: String,
    val spoken: String,
)

@Composable
private fun rememberTimeParts(timeMillis: Long): TimeParts {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    val minute = timeMillis / MINUTE_MILLIS
    return remember(minute, locale, is24Hour) {
        val date = Date(timeMillis)
        val time = SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm", locale).format(date)
        val meridiem = if (is24Hour) null else SimpleDateFormat("a", locale).format(date)
        val datePattern = DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd")
        val dateText = SimpleDateFormat(datePattern, locale).format(date)
        TimeParts(
            time = time,
            meridiem = meridiem,
            date = dateText,
            spoken = listOfNotNull(time, meridiem, dateText).joinToString(" "),
        )
    }
}

@Composable
private fun rememberAlarmLabel(alarmMillis: Long): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(alarmMillis, locale, is24Hour) {
        val pattern = DateFormat.getBestDateTimePattern(locale, if (is24Hour) "EHm" else "Ehma")
        SimpleDateFormat(pattern, locale).format(Date(alarmMillis))
    }
}

@Composable
internal fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PuiSurface(
        modifier = modifier.fillMaxWidth(),
        shape = PuiTheme.shapes.pill,
        color = PuiTheme.colors.surface,
        elevation = PuiTheme.elevation.level2,
        onClick = onClick,
        role = Role.Button,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = PuiTheme.spacing.lg, vertical = PuiTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PuiIcon(BricksIcons.Search, contentDescription = null)
            Spacer(Modifier.width(PuiTheme.spacing.md))
            PuiText(
                text = stringResource(R.string.home_search_hint),
                style = PuiTheme.typography.bodyLarge,
                color = PuiTheme.colors.onSurfaceMuted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            PuiIcon(BricksIcons.Apps, contentDescription = null, size = 20.dp, tint = PuiTheme.colors.primary)
        }
    }
}

@Composable
internal fun PageIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 1) {
        Spacer(modifier.height(INDICATOR_HEIGHT))
        return
    }
    val description = stringResource(R.string.home_a11y_page, pagerState.currentPage + 1, pageCount)
    Row(
        modifier = modifier
            .height(INDICATOR_HEIGHT)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = pagerState.currentPage == index
            val width by animateDpAsState(
                targetValue = if (selected) 22.dp else 8.dp,
                animationSpec = PuiTheme.motion.spec(PuiTheme.motion.quick),
                label = "PageDot",
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .background(if (selected) PuiTheme.colors.primary else PuiTheme.colors.surface, PuiTheme.shapes.pill)
                    .border(1.5.dp, PuiTheme.colors.ink, PuiTheme.shapes.pill),
            )
        }
    }
}

@Composable
internal fun DropZones(
    visible: Boolean,
    showUninstall: Boolean,
    hover: HomeDropTarget,
    resolver: HomeDropResolver,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(visible) {
        onDispose {
            resolver.removeZone = null
            resolver.uninstallZone = null
        }
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PuiTheme.spacing.lg, vertical = PuiTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md),
        ) {
            DropZone(
                label = stringResource(R.string.home_drop_remove),
                icon = BricksIcons.Close,
                active = hover == HomeDropTarget.Remove,
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { resolver.removeZone = it },
            )
            if (showUninstall) {
                DropZone(
                    label = stringResource(R.string.home_drop_uninstall),
                    icon = BricksIcons.Trash,
                    active = hover == HomeDropTarget.Uninstall,
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { resolver.uninstallZone = it },
                )
            }
        }
    }
}

@Composable
private fun DropZone(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    PuiSurface(
        modifier = modifier,
        shape = PuiTheme.shapes.medium,
        color = if (active) PuiTheme.colors.danger else PuiTheme.colors.surface,
        contentColor = if (active) PuiTheme.colors.onDanger else PuiTheme.colors.onSurface,
        elevation = if (active) PuiTheme.elevation.level3 else PuiTheme.elevation.level1,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PuiTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PuiIcon(icon, contentDescription = null, size = 20.dp)
            PuiText(text = label.uppercase(), style = PuiTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun DefaultLauncherPrompt(onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    PuiCard(modifier = modifier.fillMaxWidth(), elevation = PuiTheme.elevation.level3) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md)) {
            PuiIcon(BricksIcons.Home, contentDescription = null, tint = PuiTheme.colors.primary)
            PuiText(stringResource(R.string.home_default_title), style = PuiTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(PuiTheme.spacing.xs))
        PuiText(
            text = stringResource(R.string.home_default_message),
            style = PuiTheme.typography.bodySmall,
            color = PuiTheme.colors.onSurfaceMuted,
        )
        Spacer(Modifier.height(PuiTheme.spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm, Alignment.End), modifier = Modifier.fillMaxWidth()) {
            PuiTextButton(onClick = { onIntent(HomeIntent.DismissDefaultPrompt) }) {
                PuiText(stringResource(R.string.home_default_later))
            }
            PuiButton(onClick = { onIntent(HomeIntent.SetAsDefaultLauncher) }) {
                PuiText(stringResource(R.string.home_default_action))
            }
        }
    }
}

internal val INDICATOR_HEIGHT = 20.dp
private const val MINUTE_MILLIS = 60_000L
