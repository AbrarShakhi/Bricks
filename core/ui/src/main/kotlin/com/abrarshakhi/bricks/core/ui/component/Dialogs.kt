package com.abrarshakhi.bricks.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.abrarshakhi.bricks.core.ui.R
import com.abrarshakhi.paperui.PuiButton
import com.abrarshakhi.paperui.PuiButtonDefaults
import com.abrarshakhi.paperui.PuiDialog
import com.abrarshakhi.paperui.PuiText
import com.abrarshakhi.paperui.PuiTextButton
import com.abrarshakhi.paperui.PuiTextField

@Composable
fun TextInputDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    resetLabel: String? = null,
    onReset: (() -> Unit)? = null,
) {
    var value by rememberSaveable { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    PuiDialog(
        onDismissRequest = onDismiss,
        title = { PuiText(title) },
        text = {
            PuiTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                placeholder = { PuiText(placeholder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        dismissButton = {
            if (onReset != null && resetLabel != null) {
                PuiTextButton(onClick = onReset) { PuiText(resetLabel) }
            } else {
                PuiTextButton(onClick = onDismiss) { PuiText(stringResource(R.string.ui_action_cancel)) }
            }
        },
        confirmButton = {
            PuiButton(onClick = { onConfirm(value) }) { PuiText(stringResource(R.string.ui_action_save)) }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = false,
) {
    PuiDialog(
        onDismissRequest = onDismiss,
        title = { PuiText(title) },
        text = { PuiText(message) },
        dismissButton = {
            PuiTextButton(onClick = onDismiss) { PuiText(stringResource(R.string.ui_action_cancel)) }
        },
        confirmButton = {
            PuiButton(
                onClick = onConfirm,
                colors = if (destructive) PuiButtonDefaults.danger() else PuiButtonDefaults.filled(),
            ) { PuiText(confirmLabel) }
        },
    )
}
