package com.abrarshakhi.bricks.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.paperui.PuiCard
import com.abrarshakhi.paperui.PuiChip
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiIconButton
import com.abrarshakhi.paperui.PuiListItem
import com.abrarshakhi.paperui.PuiListItemDefaults
import com.abrarshakhi.paperui.PuiSlider
import com.abrarshakhi.paperui.PuiSwitch
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme

@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    stateOn: String = "On",
    stateOff: String = "Off",
) {
    PuiListItem(
        headline = { PuiText(title) },
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = if (checked) stateOn else stateOff },
        supporting = subtitle?.let { { PuiText(it) } },
        leading = icon?.let { { PuiIcon(it, contentDescription = null) } },
        trailing = { PuiSwitch(checked = checked, onCheckedChange = null, enabled = enabled) },
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
    )
}

@Composable
fun SettingNavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    value: String? = null,
) {
    PuiListItem(
        headline = { PuiText(title) },
        modifier = modifier.fillMaxWidth(),
        supporting = subtitle?.let { { PuiText(it) } },
        leading = icon?.let { { PuiIcon(it, contentDescription = null) } },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.xs),
            ) {
                if (value != null) {
                    PuiText(
                        text = value,
                        style = PuiTheme.typography.labelMedium,
                        color = PuiTheme.colors.onSurfaceMuted,
                    )
                }
                PuiIcon(BricksIcons.ChevronRight, contentDescription = null, size = 20.dp)
            }
        },
        onClick = onClick,
    )
}

@Composable
fun SettingSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueLabel: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    icon: ImageVector? = null,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    PuiCard(
        modifier = modifier.fillMaxWidth(),
        elevation = PuiTheme.elevation.level1,
        shape = PuiTheme.shapes.small,
        contentPadding = PuiListItemDefaults.ContentPadding,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md),
        ) {
            if (icon != null) PuiIcon(icon, contentDescription = null)
            PuiText(text = title, style = PuiTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            PuiText(text = valueLabel, style = PuiTheme.typography.labelLarge, color = PuiTheme.colors.primary)
        }
        Spacer(Modifier.height(PuiTheme.spacing.md))
        PuiSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SettingStepperRow(
    title: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    decreaseLabel: String = "Decrease",
    increaseLabel: String = "Increase",
) {
    PuiListItem(
        headline = { PuiText(title) },
        modifier = modifier.fillMaxWidth(),
        supporting = subtitle?.let { { PuiText(it) } },
        leading = icon?.let { { PuiIcon(it, contentDescription = null) } },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.xs),
            ) {
                PuiIconButton(
                    onClick = { onValueChange(value - 1) },
                    enabled = value > range.first,
                    size = 40.dp,
                ) { PuiIcon(BricksIcons.Minus, contentDescription = decreaseLabel, size = 20.dp) }
                PuiText(
                    text = value.toString(),
                    style = PuiTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 28.dp),
                )
                PuiIconButton(
                    onClick = { onValueChange(value + 1) },
                    enabled = value < range.last,
                    size = 40.dp,
                ) { PuiIcon(BricksIcons.Plus, contentDescription = increaseLabel, size = 20.dp) }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SettingChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable (T) -> Unit)? = null,
) {
    PuiCard(
        modifier = modifier.fillMaxWidth(),
        elevation = PuiTheme.elevation.level1,
        shape = PuiTheme.shapes.small,
        contentPadding = PuiListItemDefaults.ContentPadding,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.md),
        ) {
            if (icon != null) PuiIcon(icon, contentDescription = null)
            PuiText(text = title, style = PuiTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        }
        if (subtitle != null) {
            PuiText(
                text = subtitle,
                style = PuiTheme.typography.bodySmall,
                color = PuiTheme.colors.onSurfaceMuted,
                modifier = Modifier.padding(top = PuiTheme.spacing.xxs),
            )
        }
        Spacer(Modifier.height(PuiTheme.spacing.md))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm),
        ) {
            options.forEach { option ->
                PuiChip(
                    onClick = { onSelect(option) },
                    selected = option == selected,
                    leading = leading?.let { { it(option) } },
                ) { PuiText(label(option)) }
            }
        }
    }
}
