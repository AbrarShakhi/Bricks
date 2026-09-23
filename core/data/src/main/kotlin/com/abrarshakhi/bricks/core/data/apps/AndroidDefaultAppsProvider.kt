package com.abrarshakhi.bricks.core.data.apps

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import com.abrarshakhi.bricks.core.domain.gateway.DefaultAppsProvider
import com.abrarshakhi.bricks.core.domain.model.AppKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidDefaultAppsProvider(
    private val context: Context,
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
) : DefaultAppsProvider {

    override suspend fun dockApps(): List<AppKey> = withContext(Dispatchers.IO) {
        listOfNotNull(
            resolve(Intent(Intent.ACTION_DIAL)),
            smsPackage()?.let(::launcherActivity) ?: resolve(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
            resolve(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))),
            resolve(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) ?: resolve(Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
            category(Intent.CATEGORY_APP_GALLERY),
            category(Intent.CATEGORY_APP_CONTACTS),
            category(Intent.CATEGORY_APP_EMAIL),
        ).distinct()
    }

    override suspend fun homeApps(): List<AppKey> = withContext(Dispatchers.IO) {
        listOfNotNull(
            launcherActivity(PLAY_STORE),
            resolve(Intent(AlarmClock.ACTION_SHOW_ALARMS)),
            category(Intent.CATEGORY_APP_CALENDAR),
            category(Intent.CATEGORY_APP_MAPS),
            category(Intent.CATEGORY_APP_MUSIC),
            resolve(Intent(Settings.ACTION_SETTINGS)),
            resolve(Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH)),
        ).distinct()
    }

    private fun smsPackage(): String? = runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()

    private fun category(category: String): AppKey? =
        resolve(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category))

    private fun resolve(intent: Intent): AppKey? {
        val packageManager = context.packageManager
        val resolved = runCatching {
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrNull()?.activityInfo?.packageName
        val packageName = resolved?.takeUnless { it == ANDROID_PACKAGE }
            ?: runCatching { packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY) }
                .getOrDefault(emptyList())
                .firstOrNull { it.activityInfo.packageName != ANDROID_PACKAGE }
                ?.activityInfo?.packageName
            ?: return null
        return launcherActivity(packageName)
    }

    private fun launcherActivity(packageName: String): AppKey? {
        val user = Process.myUserHandle()
        val info = runCatching { launcherApps.getActivityList(packageName, user) }
            .getOrDefault(emptyList())
            .firstOrNull() ?: return null
        return AppKey(info.componentName.packageName, info.componentName.className, userManager.getSerialNumberForUser(user))
    }

    private companion object {
        const val ANDROID_PACKAGE = "android"
        const val PLAY_STORE = "com.android.vending"
    }
}
