package com.abrarshakhi.bricks.core.data.system

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import java.lang.ref.WeakReference

class ForegroundActivityTracker(application: Application) : Application.ActivityLifecycleCallbacks {

    private var reference: WeakReference<Activity>? = null

    val activity: Activity?
        get() = reference?.get()?.takeUnless { it.isFinishing || it.isDestroyed }

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    fun start(context: Context, intent: Intent): Boolean {
        val host = activity
        return runCatching {
            if (host != null) {
                host.startActivity(intent)
            } else {
                context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            true
        }.getOrDefault(false)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        reference = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        reference = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (reference?.get() === activity) reference = null
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
