package com.abrarshakhi.bricks.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.abrarshakhi.paperui.PuiHorizontalDivider
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTheme
import com.abrarshakhi.paperui.puiHalftone

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = PuiTheme.spacing.sm, bottom = PuiTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(PuiTheme.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PuiText(
                text = title.uppercase(),
                style = PuiTheme.typography.labelMedium,
                color = PuiTheme.colors.onSurfaceMuted,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (action != null) action()
        }
        PuiHorizontalDivider(
            modifier = Modifier.puiHalftone(color = PuiTheme.colors.ink),
            thickness = PuiTheme.stroke.regular,
        )
    }
}
