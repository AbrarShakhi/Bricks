package com.abrarshakhi.bricks.feature.widgets

import android.content.Intent
import androidx.compose.runtime.Immutable
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.WidgetProvider
import com.abrarshakhi.bricks.core.domain.usecase.system.WidgetProviderGroup

@Immutable
data class WidgetsState(
    val loading: Boolean = true,
    val query: String = "",
    val groups: List<WidgetProviderGroup> = emptyList(),
    val grid: GridSize = GridSize(4, 5),
    val adding: Boolean = false,
)

sealed interface WidgetsIntent {
    data class QueryChanged(val query: String) : WidgetsIntent
    data class Add(val provider: WidgetProvider, val cellWidthDp: Float, val cellHeightDp: Float) : WidgetsIntent
    data class BindResult(val granted: Boolean) : WidgetsIntent
}

enum class WidgetsMessage { Failed, Cancelled }

sealed interface WidgetsEffect {
    data class RequestBind(val intent: Intent) : WidgetsEffect
    data class Configure(val appWidgetId: Int, val requestCode: Int) : WidgetsEffect
    data object Added : WidgetsEffect
    data class ShowMessage(val message: WidgetsMessage) : WidgetsEffect
}
