package com.abrarshakhi.bricks.core.data.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.ProfileType
import com.abrarshakhi.bricks.core.domain.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn

internal data class InstalledApp(
    val key: AppKey,
    val label: String,
    val installedAt: Long,
    val isSystem: Boolean,
    val isSuspended: Boolean,
    val profile: ProfileType,
)

internal class LauncherAppsCatalog(
    private val context: Context,
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
    monitor: PackageChangeMonitor,
    appScope: CoroutineScope,
) {
    val apps: Flow<List<InstalledApp>> = monitor.changes
        .onStart { emit(Unit) }
        .conflate()
        .map { loadApps() }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    val profiles: Flow<List<UserProfile>> = monitor.changes
        .onStart { emit(Unit) }
        .conflate()
        .map { loadProfiles() }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    private fun loadApps(): List<InstalledApp> {
        val ownPackage = context.packageName
        return userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            val profile = profileType(user)
            val paused = isPaused(user)
            val activities = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
            activities.mapNotNull { info ->
                val component = info.componentName
                if (component.packageName == ownPackage) return@mapNotNull null
                val flags = info.applicationInfo.flags
                InstalledApp(
                    key = AppKey(component.packageName, component.className, serial),
                    label = info.label?.toString()?.trim().orEmpty().ifEmpty { component.packageName },
                    installedAt = info.firstInstallTime,
                    isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isSuspended = paused || (flags and ApplicationInfo.FLAG_SUSPENDED) != 0,
                    profile = profile,
                )
            }
        }
    }

    private fun loadProfiles(): List<UserProfile> = userManager.userProfiles.map { user ->
        UserProfile(
            serial = userManager.getSerialNumberForUser(user),
            type = profileType(user),
            isPaused = isPaused(user),
        )
    }

    private fun isPaused(user: UserHandle): Boolean =
        user != Process.myUserHandle() && runCatching { userManager.isQuietModeEnabled(user) }.getOrDefault(false)

    private fun profileType(user: UserHandle): ProfileType = when {
        user == Process.myUserHandle() -> ProfileType.Personal
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && isPrivateProfile(user) -> ProfileType.Private
        else -> ProfileType.Work
    }

    private fun isPrivateProfile(user: UserHandle): Boolean = runCatching {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
            launcherApps.getLauncherUserInfo(user)?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
    }.getOrDefault(false)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
