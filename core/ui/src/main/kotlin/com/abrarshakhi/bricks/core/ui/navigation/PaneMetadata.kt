package com.abrarshakhi.bricks.core.ui.navigation

object PaneMetadata {
    const val ROLE = "bricks.pane.role"
    const val LIST = "list"
    const val DETAIL = "detail"

    val List: Map<String, Any> = mapOf(ROLE to LIST)
    val Detail: Map<String, Any> = mapOf(ROLE to DETAIL)
}
