package com.abrarshakhi.bricks.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.paperui.PuiBubbleTail
import com.abrarshakhi.paperui.PuiButtonDefaults
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiIconButton
import com.abrarshakhi.paperui.PuiSpeechBubble
import com.abrarshakhi.paperui.PuiSurface
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTextField
import com.abrarshakhi.paperui.PuiTheme

@Composable
fun ComicCaption(
    text: String,
    modifier: Modifier = Modifier,
) {
    PuiSurface(
        modifier = modifier,
        shape = PuiTheme.shapes.none,
        color = PuiTheme.colors.accent,
        contentColor = PuiTheme.colors.onAccent,
        elevation = PuiTheme.elevation.level1,
    ) {
        PuiText(
            text = text.uppercase(),
            style = PuiTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = PuiTheme.spacing.sm, vertical = PuiTheme.spacing.xs),
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(PuiTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.lg),
    ) {
        PuiSpeechBubble(
            modifier = Modifier.widthIn(max = 320.dp),
            tail = PuiBubbleTail.Bottom,
            tailBias = 0.5f,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.xs),
            ) {
                if (icon != null) PuiIcon(icon, contentDescription = null, size = 32.dp)
                PuiText(
                    text = title,
                    style = PuiTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                if (message != null) {
                    PuiText(
                        text = message,
                        style = PuiTheme.typography.bodyMedium,
                        color = PuiTheme.colors.onSurfaceMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (action != null) action()
    }
}

@Composable
fun InkSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    clearLabel: String = "Clear",
    onSearch: (() -> Unit)? = null,
) {
    PuiTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { PuiText(placeholder, maxLines = 1) },
        leading = { PuiIcon(BricksIcons.Search, contentDescription = null) },
        trailing = if (value.isNotEmpty()) {
            {
                PuiIconButton(
                    onClick = { onValueChange("") },
                    size = 32.dp,
                    elevation = 0.dp,
                    borderWidth = 0.dp,
                    colors = PuiButtonDefaults.text(content = PuiTheme.colors.onSurface),
                ) { PuiIcon(BricksIcons.Close, contentDescription = clearLabel, size = 18.dp) }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
    )
}
