package com.abrarshakhi.bricks.feature.home.pin

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey

internal class PlatformPinItemRequest(
    context: Context,
    private val request: LauncherApps.PinItemRequest?,
) : PinItemRequestHandle {

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)

    override val candidate: PinCandidate? = request
        ?.takeIf { runCatching { it.isValid }.getOrDefault(false) }
        ?.let { runCatching { describe(it) }.getOrNull() }

    override fun acceptShortcut(): Boolean = runCatching {
        request != null && request.isValid && request.accept()
    }.getOrDefault(false)

    override fun acceptWidget(appWidgetId: Int): Boolean = runCatching {
        val options = Bundle().apply { putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) }
        request != null && request.isValid && request.accept(options)
    }.getOrDefault(false)

    private fun describe(request: LauncherApps.PinItemRequest): PinCandidate? = when (request.requestType) {
        LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT -> request.shortcutInfo?.let(::shortcutCandidate)
        LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET -> request.getAppWidgetProviderInfo(appContext)?.let { info ->
            val provider = info.provider
            val appLabel = appLabel(provider.packageName, info.profile)
            PinCandidate.Widget(
                provider = WidgetProviderKey(provider.packageName, provider.className, userManager.getSerialNumberForUser(info.profile)),
                label = info.loadLabel(appContext.packageManager)?.toString()?.trim().orEmpty().ifEmpty { appLabel },
                appLabel = appLabel,
            )
        }
        else -> null
    }

    private fun shortcutCandidate(info: ShortcutInfo): PinCandidate.Shortcut {
        val appLabel = appLabel(info.`package`, info.userHandle)
        val label = (info.shortLabel ?: info.longLabel)?.toString()?.trim().orEmpty().ifEmpty { appLabel }
        return PinCandidate.Shortcut(
            key = ShortcutKey(info.`package`, info.id, userManager.getSerialNumberForUser(info.userHandle)),
            label = label,
            appLabel = appLabel,
            icon = PinIconSource { densityDpi -> launcherApps.getShortcutIconDrawable(info, densityDpi) },
        )
    }

    private fun appLabel(packageName: String, user: UserHandle): String = runCatching {
        appContext.packageManager.getApplicationLabel(launcherApps.getApplicationInfo(packageName, 0, user)).toString()
    }.getOrDefault(packageName)
}
