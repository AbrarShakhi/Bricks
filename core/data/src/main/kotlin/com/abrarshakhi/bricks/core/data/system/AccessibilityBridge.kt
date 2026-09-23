package com.abrarshakhi.bricks.core.data.system

import android.accessibilityservice.AccessibilityService
import java.lang.ref.WeakReference

class AccessibilityBridge {
    private var reference: WeakReference<AccessibilityService>? = null

    val isConnected: Boolean
        get() = reference?.get() != null

    internal fun attach(service: AccessibilityService) {
        reference = WeakReference(service)
    }

    internal fun detach(service: AccessibilityService) {
        if (reference?.get() === service) reference = null
    }

    fun perform(action: Int): Boolean = reference?.get()?.performGlobalAction(action) ?: false
}
