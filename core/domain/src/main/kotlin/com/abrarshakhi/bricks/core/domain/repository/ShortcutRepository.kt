package com.abrarshakhi.bricks.core.domain.repository

import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppShortcut
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey

interface ShortcutRepository {
    val isAvailable: Boolean

    suspend fun shortcutsFor(app: AppKey): List<AppShortcut>

    suspend fun pinnedShortcuts(keys: Set<ShortcutKey>): List<AppShortcut>

    fun launch(shortcut: ShortcutKey, bounds: ScreenBounds?): Boolean

    suspend fun pin(shortcut: ShortcutKey)

    suspend fun unpin(shortcut: ShortcutKey)
}
