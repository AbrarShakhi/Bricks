package com.abrarshakhi.bricks.core.ui.widget

import android.content.Intent
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey

interface WidgetBinder {
    fun allocateId(): Int

    fun deleteId(appWidgetId: Int)

    fun allocatedIds(): Set<Int>

    fun bindIfAllowed(appWidgetId: Int, provider: WidgetProviderKey): Boolean

    fun bindPermissionIntent(appWidgetId: Int, provider: WidgetProviderKey): Intent?

    fun needsConfiguration(appWidgetId: Int): Boolean
}
