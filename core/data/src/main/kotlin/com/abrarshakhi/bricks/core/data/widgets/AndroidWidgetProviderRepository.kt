package com.abrarshakhi.bricks.core.data.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.abrarshakhi.bricks.core.data.apps.PackageChangeMonitor
import com.abrarshakhi.bricks.core.domain.model.ProfileType
import com.abrarshakhi.bricks.core.domain.model.WidgetProvider
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import com.abrarshakhi.bricks.core.domain.repository.WidgetProviderRepository
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
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal class AndroidWidgetProviderRepository(
    private val context: Context,
    private val appWidgetManager: AppWidgetManager,
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
    monitor: PackageChangeMonitor,
    appScope: CoroutineScope,
) : WidgetProviderRepository {

    override val providers: Flow<List<WidgetProvider>> = monitor.changes
        .onStart { emit(Unit) }
        .conflate()
        .map { load() }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    override suspend fun find(key: WidgetProviderKey): WidgetProvider? = withContext(Dispatchers.IO) {
        load().firstOrNull { it.key == key }
    }

    private fun load(): List<WidgetProvider> {
        val density = context.resources.displayMetrics.density
        return userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            val profile = if (user == Process.myUserHandle()) ProfileType.Personal else ProfileType.Work
            runCatching { appWidgetManager.getInstalledProvidersForProfile(user) }
                .getOrDefault(emptyList())
                .filterNot { it.isHiddenFromPicker() }
                .mapNotNull { info -> runCatching { info.toDomain(serial, profile, density, user) }.getOrNull() }
        }
    }

    private fun AppWidgetProviderInfo.isHiddenFromPicker(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            (widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_HIDE_FROM_PICKER) != 0

    private fun AppWidgetProviderInfo.toDomain(
        serial: Long,
        profile: ProfileType,
        density: Float,
        user: UserHandle,
    ): WidgetProvider {
        val packageManager = context.packageManager
        val packageName = provider.packageName
        val appLabel = runCatching {
            packageManager.getApplicationLabel(launcherApps.getApplicationInfo(packageName, 0, user)).toString()
        }.getOrDefault(packageName)
        val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val optionalConfig = modern && (widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
        return WidgetProvider(
            key = WidgetProviderKey(packageName, provider.className, serial),
            label = loadLabel(packageManager)?.toString()?.trim().orEmpty().ifEmpty { appLabel },
            appLabel = appLabel,
            description = if (modern) loadDescription(context)?.toString() else null,
            profile = profile,
            minWidthDp = minWidth.toDp(density),
            minHeightDp = minHeight.toDp(density),
            minResizeWidthDp = minResizeWidth.toDp(density),
            minResizeHeightDp = minResizeHeight.toDp(density),
            maxResizeWidthDp = if (modern) maxResizeWidth.toDp(density).takeIf { it > 0 } else null,
            maxResizeHeightDp = if (modern) maxResizeHeight.toDp(density).takeIf { it > 0 } else null,
            targetCellWidth = if (modern) targetCellWidth.takeIf { it > 0 } else null,
            targetCellHeight = if (modern) targetCellHeight.takeIf { it > 0 } else null,
            resizeHorizontal = (resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0,
            resizeVertical = (resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL) != 0,
            isConfigurable = configure != null && !optionalConfig,
        )
    }

    private fun Int.toDp(density: Float): Int = (this / density).roundToInt()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
