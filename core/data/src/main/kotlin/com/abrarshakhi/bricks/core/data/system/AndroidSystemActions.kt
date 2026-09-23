package com.abrarshakhi.bricks.core.data.system

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.abrarshakhi.bricks.core.data.badges.BadgeListenerService
import com.abrarshakhi.bricks.core.domain.gateway.SystemActions

internal class AndroidSystemActions(
    private val context: Context,
    private val activityTracker: ForegroundActivityTracker,
    private val bridge: AccessibilityBridge,
) : SystemActions {

    override val isLockScreenReady: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bridge.isConnected

    override val isDefaultLauncher: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                }
            }
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            return resolved?.activityInfo?.packageName == context.packageName
        }

    override fun expandNotifications(): Boolean =
        bridge.perform(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) || invokeStatusBar("expandNotificationsPanel")

    override fun expandQuickSettings(): Boolean =
        bridge.perform(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS) || invokeStatusBar("expandSettingsPanel")

    override fun lockScreen(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bridge.perform(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

    override fun openWallpaperPicker(): Boolean =
        activityTracker.start(context, Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), null))

    override fun openLockScreenSetup(): Boolean =
        activityTracker.start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    override fun openNotificationAccessSettings(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val details = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, BadgeListenerService::class.java).flattenToString(),
                )
            if (activityTracker.start(context, details)) return true
        }
        return activityTracker.start(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    override fun openDefaultLauncherSettings(): Boolean {
        val activity = activityTracker.activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity != null) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                val requested = runCatching {
                    activity.startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME), ROLE_REQUEST_CODE)
                    true
                }.getOrDefault(false)
                if (requested) return true
            }
        }
        return activityTracker.start(context, Intent(Settings.ACTION_HOME_SETTINGS))
    }

    @SuppressLint("WrongConstant")
    private fun invokeStatusBar(method: String): Boolean = runCatching {
        val service = context.getSystemService(STATUS_BAR_SERVICE) ?: return false
        Class.forName("android.app.StatusBarManager").getMethod(method).invoke(service)
        true
    }.getOrDefault(false)

    private companion object {
        const val STATUS_BAR_SERVICE = "statusbar"
        const val ROLE_REQUEST_CODE = 4127
    }
}
