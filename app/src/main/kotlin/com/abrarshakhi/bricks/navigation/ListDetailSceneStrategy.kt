package com.abrarshakhi.bricks.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.abrarshakhi.bricks.core.ui.navigation.PaneMetadata
import com.abrarshakhi.paperui.PuiTheme
import com.abrarshakhi.paperui.PuiVerticalDivider

class ListDetailSceneStrategy<T : Any>(
    private val enabled: Boolean,
    private val placeholder: @Composable () -> Unit,
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (!enabled) return null
        val last = entries.lastOrNull() ?: return null
        val role = last.metadata[PaneMetadata.ROLE]
        if (role == PaneMetadata.LIST) {
            return ListDetailScene(
                key = last.contentKey,
                list = last,
                detail = null,
                previousEntries = entries.dropLast(1),
                placeholder = placeholder,
            )
        }
        val previous = entries.getOrNull(entries.lastIndex - 1) ?: return null
        if (role == PaneMetadata.DETAIL && previous.metadata[PaneMetadata.ROLE] == PaneMetadata.LIST) {
            return ListDetailScene(
                key = previous.contentKey,
                list = previous,
                detail = last,
                previousEntries = entries.dropLast(1),
                placeholder = placeholder,
            )
        }
        return null
    }
}

private class ListDetailScene<T : Any>(
    override val key: Any,
    private val list: NavEntry<T>,
    private val detail: NavEntry<T>?,
    override val previousEntries: List<NavEntry<T>>,
    private val placeholder: @Composable () -> Unit,
) : Scene<T> {

    override val entries: List<NavEntry<T>> = listOfNotNull(list, detail)

    override val content: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxSize().background(PuiTheme.colors.paper)) {
            Box(modifier = Modifier.weight(LIST_WEIGHT).fillMaxHeight()) { list.Content() }
            PuiVerticalDivider(thickness = PuiTheme.stroke.bold)
            Box(modifier = Modifier.weight(1f - LIST_WEIGHT).fillMaxHeight()) {
                if (detail != null) {
                    key(detail.contentKey) { detail.Content() }
                } else {
                    placeholder()
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListDetailScene<*>) return false
        return key == other.key && list == other.list && detail == other.detail && previousEntries == other.previousEntries
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + list.hashCode()
        result = 31 * result + (detail?.hashCode() ?: 0)
        result = 31 * result + previousEntries.hashCode()
        return result
    }

    private companion object {
        const val LIST_WEIGHT = 0.4f
    }
}
