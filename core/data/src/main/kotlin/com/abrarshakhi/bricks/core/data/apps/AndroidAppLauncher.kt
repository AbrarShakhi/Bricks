package com.abrarshakhi.bricks.core.data.apps

import android.app.ActivityOptions
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.UserManager
import com.abrarshakhi.bricks.core.data.system.ForegroundActivityTracker
import com.abrarshakhi.bricks.core.domain.gateway.AppLauncher
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds

internal class AndroidAppLauncher(
    private val context: Context,
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
    private val activityTracker: ForegroundActivityTracker,
) : AppLauncher {

    override fun launch(app: AppKey, bounds: ScreenBounds?): Boolean {
        val user = userManager.userFor(app.userSerial) ?: return false
        return runCatching {
            launcherApps.startMainActivity(
                ComponentName(app.packageName, app.className),
                user,
                bounds?.toRect(),
                revealOptions(bounds),
            )
            true
        }.getOrDefault(false)
    }

    override fun openAppInfo(app: AppKey, bounds: ScreenBounds?): Boolean {
        val user = userManager.userFor(app.userSerial) ?: return false
        return runCatching {
            launcherApps.startAppDetailsActivity(
                ComponentName(app.packageName, app.className),
                user,
                bounds?.toRect(),
                revealOptions(bounds),
            )
            true
        }.getOrDefault(false)
    }

    override fun requestUninstall(app: AppKey): Boolean {
        val user = userManager.userFor(app.userSerial) ?: return false
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, app.className))
            .putExtra(Intent.EXTRA_USER, user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        return activityTracker.start(context, intent)
    }

    override fun openStoreListing(packageName: String): Boolean =
        activityTracker.start(context, Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))) ||
            activityTracker.start(
                context,
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")),
            )

    override fun searchWeb(query: String): Boolean {
        val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        if (search.resolveActivity(context.packageManager) != null && activityTracker.start(context, search)) return true
        val url = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        return activityTracker.start(context, Intent(Intent.ACTION_VIEW, url))
    }

    override fun searchStore(query: String): Boolean =
        activityTracker.start(context, Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(query)}&c=apps"))) ||
            activityTracker.start(
                context,
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=${Uri.encode(query)}&c=apps")),
            )

    private fun revealOptions(bounds: ScreenBounds?): Bundle? {
        val source = activityTracker.activity?.window?.decorView ?: return null
        if (bounds == null) return null
        return ActivityOptions.makeClipRevealAnimation(
            source,
            bounds.left,
            bounds.top,
            bounds.right - bounds.left,
            bounds.bottom - bounds.top,
        ).toBundle()
    }
}
