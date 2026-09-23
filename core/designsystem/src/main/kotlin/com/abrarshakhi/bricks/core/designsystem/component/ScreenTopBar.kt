package com.abrarshakhi.bricks.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abrarshakhi.bricks.core.designsystem.icon.BricksIcons
import com.abrarshakhi.paperui.PuiIcon
import com.abrarshakhi.paperui.PuiIconButton
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme
import com.abrarshakhi.paperui.PuiTopBar

@Composable
fun ScreenTopBar(
    title: String,
    onBack: (() -> Unit)?,
    backContentDescription: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PuiTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        PuiTopBar(
            title = { PuiText(title, maxLines = 1) },
            navigationIcon = onBack?.let { back ->
                {
                    PuiIconButton(onClick = back, size = 44.dp) {
                        PuiIcon(BricksIcons.ArrowBack, contentDescription = backContentDescription)
                    }
                }
            },
            actions = actions,
        )
    }
}

val ScreenBodyInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
