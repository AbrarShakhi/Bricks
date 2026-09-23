package com.abrarshakhi.bricks.core.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey

class LauncherWidgetHost(
    context: Context,
    private val userManager: UserManager,
) : WidgetBinder {
    private val appContext = context.applicationContext
    private val manager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    private var listening = false

    private val host = object : AppWidgetHost(appContext, HOST_ID) {
        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?,
        ): AppWidgetHostView = LauncherWidgetHostView(context)
    }

    fun startListening() {
        if (listening) return
        listening = runCatching { host.startListening() }.isSuccess
    }

    fun stopListening() {
        if (!listening) return
        runCatching { host.stopListening() }
        listening = false
    }

    override fun allocateId(): Int = host.allocateAppWidgetId()

    override fun deleteId(appWidgetId: Int) {
        runCatching { host.deleteAppWidgetId(appWidgetId) }
    }

    override fun allocatedIds(): Set<Int> = runCatching { host.appWidgetIds.toSet() }.getOrDefault(emptySet())

    override fun bindIfAllowed(appWidgetId: Int, provider: WidgetProviderKey): Boolean {
        val user = runCatching { userManager.getUserForSerialNumber(provider.userSerial) }.getOrNull() ?: return false
        return runCatching {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, user, provider.component(), null)
        }.getOrDefault(false)
    }

    override fun bindPermissionIntent(appWidgetId: Int, provider: WidgetProviderKey): Intent? {
        val user = runCatching { userManager.getUserForSerialNumber(provider.userSerial) }.getOrNull() ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.component())
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, user)
    }

    fun info(appWidgetId: Int): AppWidgetProviderInfo? = runCatching { manager.getAppWidgetInfo(appWidgetId) }.getOrNull()

    override fun needsConfiguration(appWidgetId: Int): Boolean {
        val info = info(appWidgetId) ?: return false
        if (info.configure == null) return false
        val optional = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
        return !optional
    }

    fun canReconfigure(appWidgetId: Int): Boolean {
        val info = info(appWidgetId) ?: return false
        if (info.configure == null) return false
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE) != 0
    }

    fun startConfiguration(activity: Activity, appWidgetId: Int, requestCode: Int): Boolean = runCatching {
        host.startAppWidgetConfigureActivityForResult(activity, appWidgetId, 0, requestCode, null)
        true
    }.getOrDefault(false)

    fun createView(context: Context, appWidgetId: Int): LauncherWidgetHostView? {
        val info = info(appWidgetId) ?: return null
        return runCatching { host.createView(context, appWidgetId, info) as? LauncherWidgetHostView }.getOrNull()
    }

    private fun WidgetProviderKey.component() = ComponentName(packageName, className)

    companion object {
        const val HOST_ID = 0x42524B
    }
}
