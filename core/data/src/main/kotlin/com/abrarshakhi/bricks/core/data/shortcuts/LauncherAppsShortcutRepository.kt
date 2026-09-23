package com.abrarshakhi.bricks.core.data.shortcuts

import android.content.ComponentName
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import android.os.UserManager
import com.abrarshakhi.bricks.core.data.apps.toRect
import com.abrarshakhi.bricks.core.data.apps.userFor
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppShortcut
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.repository.ShortcutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class LauncherAppsShortcutRepository(
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
) : ShortcutRepository {

    override val isAvailable: Boolean
        get() = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    override suspend fun shortcutsFor(app: AppKey): List<AppShortcut> = withContext(Dispatchers.IO) {
        val user = userManager.userFor(app.userSerial) ?: return@withContext emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setActivity(ComponentName(app.packageName, app.className))
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
        query(query, user).map { it.toDomain(app.userSerial) }
    }

    override suspend fun pinnedShortcuts(keys: Set<ShortcutKey>): List<AppShortcut> = withContext(Dispatchers.IO) {
        keys.groupBy { it.packageName to it.userSerial }.flatMap { (owner, group) ->
            val user = userManager.userFor(owner.second) ?: return@flatMap emptyList()
            val query = LauncherApps.ShortcutQuery()
                .setPackage(owner.first)
                .setShortcutIds(group.map { it.shortcutId })
                .setQueryFlags(ALL_FLAGS)
            query(query, user).map { it.toDomain(owner.second) }
        }
    }

    override fun launch(shortcut: ShortcutKey, bounds: ScreenBounds?): Boolean {
        val user = userManager.userFor(shortcut.userSerial) ?: return false
        return runCatching {
            launcherApps.startShortcut(shortcut.packageName, shortcut.shortcutId, bounds?.toRect(), null, user)
            true
        }.getOrDefault(false)
    }

    override suspend fun pin(shortcut: ShortcutKey) {
        updatePins(shortcut) { it + shortcut.shortcutId }
    }

    override suspend fun unpin(shortcut: ShortcutKey) {
        updatePins(shortcut) { it - shortcut.shortcutId }
    }

    private suspend fun updatePins(shortcut: ShortcutKey, change: (Set<String>) -> Set<String>) {
        withContext(Dispatchers.IO) {
            if (!isAvailable) return@withContext
            val user = userManager.userFor(shortcut.userSerial) ?: return@withContext
            val query = LauncherApps.ShortcutQuery()
                .setPackage(shortcut.packageName)
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            val pinned = query(query, user).map { it.id }.toSet()
            runCatching { launcherApps.pinShortcuts(shortcut.packageName, change(pinned).toList(), user) }
        }
    }

    private fun query(query: LauncherApps.ShortcutQuery, user: UserHandle): List<ShortcutInfo> {
        if (!isAvailable) return emptyList()
        return runCatching { launcherApps.getShortcuts(query, user) }.getOrNull().orEmpty()
    }

    private fun ShortcutInfo.toDomain(serial: Long) = AppShortcut(
        key = ShortcutKey(`package`, id, serial),
        shortLabel = shortLabel?.toString()?.takeIf { it.isNotBlank() } ?: id,
        longLabel = longLabel?.toString(),
        rank = rank,
        isDynamic = isDynamic,
        isPinned = isPinned,
        isEnabled = isEnabled,
    )

    private companion object {
        const val ALL_FLAGS = LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
    }
}
