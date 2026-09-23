package com.abrarshakhi.bricks.feature.home.pin

import android.content.Intent
import android.graphics.drawable.Drawable
import com.abrarshakhi.bricks.core.domain.model.AppearanceSettings
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.model.WidgetProvider
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey

fun interface PinIconSource {
    fun load(densityDpi: Int): Drawable?
}

sealed interface PinCandidate {
    val label: String
    val appLabel: String

    data class Shortcut(
        val key: ShortcutKey,
        override val label: String,
        override val appLabel: String,
        val icon: PinIconSource,
    ) : PinCandidate

    data class Widget(
        val provider: WidgetProviderKey,
        override val label: String,
        override val appLabel: String,
    ) : PinCandidate
}

interface PinItemRequestHandle {
    val candidate: PinCandidate?

    fun acceptShortcut(): Boolean

    fun acceptWidget(appWidgetId: Int): Boolean
}

data class PinItemState(
    val candidate: PinCandidate? = null,
    val appearance: AppearanceSettings? = null,
    val grid: GridSize = DEFAULT_GRID,
    val provider: WidgetProvider? = null,
    val busy: Boolean = false,
) {
    val canConfirm: Boolean
        get() = !busy && when (candidate) {
            is PinCandidate.Shortcut -> true
            is PinCandidate.Widget -> provider != null
            null -> false
        }

    companion object {
        val DEFAULT_GRID: GridSize = GridSize.of(4, 5)
    }
}

sealed interface PinItemIntent {
    data class Confirm(val cellWidthDp: Float, val cellHeightDp: Float) : PinItemIntent
    data class BindResult(val granted: Boolean) : PinItemIntent
    data object Dismiss : PinItemIntent
}

enum class PinOutcome { Added, Cancelled, Failed }

sealed interface PinItemEffect {
    data class RequestBind(val intent: Intent) : PinItemEffect
    data class Finish(val outcome: PinOutcome) : PinItemEffect
}
