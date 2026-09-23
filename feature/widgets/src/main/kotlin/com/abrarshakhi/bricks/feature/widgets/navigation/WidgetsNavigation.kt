package com.abrarshakhi.bricks.feature.widgets.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.abrarshakhi.bricks.feature.widgets.ui.WidgetsRoute
import kotlinx.serialization.Serializable

@Serializable
data object WidgetPickerKey : NavKey

fun EntryProviderScope<NavKey>.widgetEntries(onBack: () -> Unit) {
    entry<WidgetPickerKey> {
        WidgetsRoute(onBack = onBack)
    }
}
