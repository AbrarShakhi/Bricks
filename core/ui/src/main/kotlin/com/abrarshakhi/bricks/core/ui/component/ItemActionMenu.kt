package com.abrarshakhi.bricks.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppShortcut
import com.abrarshakhi.bricks.core.domain.model.IconStyle
import com.abrarshakhi.bricks.core.ui.R
import com.abrarshakhi.bricks.core.ui.icon.rememberShortcutIcon
import com.abrarshakhi.paperui.PuiButtonDefaults
import com.abrarshakhi.paperui.PuiCircularProgressIndicator
import com.abrarshakhi.paperui.PuiDropdownMenu
import com.abrarshakhi.paperui.PuiDropdownMenuItem
import com.abrarshakhi.paperui.PuiHorizontalDivider
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiIconButton
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme

@Immutable
data class ItemMenuEntry<T>(
    val value: T,
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
)

@Composable
fun <T> ItemActionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    entries: List<ItemMenuEntry<T>>,
    onEntry: (T) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    shortcuts: List<AppShortcut> = emptyList(),
    shortcutsLoading: Boolean = false,
    shortcutOwner: AppKey? = null,
    iconStyle: IconStyle = IconStyle.Sticker,
    onShortcut: (AppShortcut) -> Unit = {},
    onPinShortcut: ((AppShortcut) -> Unit)? = null,
    offset: DpOffset = DpOffset(0.dp, 6.dp),
) {
    PuiDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        offset = offset,
    ) {
        if (title != null) {
            PuiText(
                text = title,
                style = PuiTheme.typography.labelMedium,
                color = PuiTheme.colors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = PuiTheme.spacing.lg, vertical = PuiTheme.spacing.xs),
            )
        }
        if (shortcutsLoading) {
            Box(modifier = Modifier.padding(PuiTheme.spacing.md).align(Alignment.CenterHorizontally)) {
                PuiCircularProgressIndicator(size = 24.dp, strokeWidth = 3.dp)
            }
        }
        shortcuts.forEach { shortcut ->
            PuiDropdownMenuItem(
                onClick = { onShortcut(shortcut) },
                leading = { ShortcutIcon(shortcut, shortcutOwner, iconStyle) },
                trailing = onPinShortcut?.let { pin ->
                    {
                        PuiIconButton(
                            onClick = { pin(shortcut) },
                            size = 36.dp,
                            elevation = 0.dp,
                            borderWidth = 0.dp,
                            colors = PuiButtonDefaults.text(content = PuiTheme.colors.onSurfaceMuted),
                        ) {
                            PuiIcon(
                                BricksIcons.Pin,
                                contentDescription = stringResource(R.string.ui_menu_pin_shortcut),
                                size = 18.dp,
                            )
                        }
                    }
                },
            ) {
                PuiText(shortcut.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (shortcuts.isNotEmpty() && entries.isNotEmpty()) {
            PuiHorizontalDivider(
                thickness = PuiTheme.stroke.hairline,
                color = PuiTheme.colors.surfaceMuted,
                modifier = Modifier.padding(vertical = PuiTheme.spacing.xs),
            )
        }
        entries.forEach { entry ->
            val tint = if (entry.destructive) PuiTheme.colors.danger else PuiTheme.colors.onSurface
            PuiDropdownMenuItem(
                onClick = { onEntry(entry.value) },
                leading = { PuiIcon(entry.icon, contentDescription = null, tint = tint, size = 22.dp) },
            ) {
                PuiText(entry.label, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ShortcutIcon(shortcut: AppShortcut, owner: AppKey?, style: IconStyle) {
    val icon = rememberShortcutIcon(shortcut.key, owner, 28.dp, if (style == IconStyle.Ink) IconStyle.Ink else IconStyle.Sticker)
    AppIconTile(icon = icon, style = style, size = 26.dp, modifier = Modifier.size(26.dp))
}
