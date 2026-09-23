package com.abrarshakhi.bricks.core.domain.model

data class AppShortcut(
    val key: ShortcutKey,
    val shortLabel: String,
    val longLabel: String?,
    val rank: Int,
    val isDynamic: Boolean,
    val isPinned: Boolean,
    val isEnabled: Boolean,
) {
    val label: String
        get() = longLabel?.takeIf { it.isNotBlank() && it.length <= MAX_MENU_LABEL } ?: shortLabel

    private companion object {
        const val MAX_MENU_LABEL = 28
    }
}
