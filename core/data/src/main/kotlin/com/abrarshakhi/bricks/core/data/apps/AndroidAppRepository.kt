package com.abrarshakhi.bricks.core.data.apps

import android.os.Build
import android.os.UserManager
import androidx.datastore.core.DataStore
import com.abrarshakhi.bricks.core.data.ApplicationScope
import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppUsage
import com.abrarshakhi.bricks.core.domain.model.UserProfile
import com.abrarshakhi.bricks.core.domain.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

internal class AndroidAppRepository(
    private val catalog: LauncherAppsCatalog,
    private val metadata: DataStore<AppMetadataDto>,
    private val userManager: UserManager,
    appScope: ApplicationScope,
) : AppRepository {

    override val apps: Flow<List<App>> = combine(catalog.apps, metadata.data) { installed, meta ->
        installed.map { app ->
            val id = app.key.flattened
            val usage = meta.usage[id]
            App(
                key = app.key,
                label = meta.labels[id] ?: app.label,
                systemLabel = app.label,
                profile = app.profile,
                installedAt = app.installedAt,
                isSystem = app.isSystem,
                isHidden = id in meta.hidden,
                isSuspended = app.isSuspended,
                usage = if (usage == null) AppUsage() else AppUsage(usage.count, usage.last),
            )
        }
    }
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    override val profiles: Flow<List<UserProfile>> = catalog.profiles

    override suspend fun setHidden(key: AppKey, hidden: Boolean) {
        metadata.updateData { dto ->
            dto.copy(hidden = if (hidden) dto.hidden + key.flattened else dto.hidden - key.flattened)
        }
    }

    override suspend fun setCustomLabel(key: AppKey, label: String?) {
        metadata.updateData { dto ->
            dto.copy(labels = if (label == null) dto.labels - key.flattened else dto.labels + (key.flattened to label))
        }
    }

    override suspend fun recordLaunch(key: AppKey, timestamp: Long) {
        metadata.updateData { dto ->
            val id = key.flattened
            val current = dto.usage[id] ?: UsageDto()
            dto.copy(usage = dto.usage + (id to UsageDto(count = current.count + 1, last = timestamp)))
        }
    }

    override suspend fun setProfilePaused(serial: Long, paused: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@withContext false
        val user = userManager.getUserForSerialNumber(serial) ?: return@withContext false
        runCatching { userManager.requestQuietModeEnabled(paused, user) }.getOrDefault(false)
    }

    override suspend fun consumeNewlyInstalled(installed: Set<AppKey>): Set<AppKey> {
        var fresh = emptySet<AppKey>()
        metadata.updateData { dto ->
            val current = installed.map { it.flattened }.toSet()
            val previous = dto.known
            fresh = if (previous == null) emptySet() else (current - previous).mapNotNull(AppKey::parse).toSet()
            if (previous == current) dto else dto.copy(known = current)
        }
        return fresh
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
