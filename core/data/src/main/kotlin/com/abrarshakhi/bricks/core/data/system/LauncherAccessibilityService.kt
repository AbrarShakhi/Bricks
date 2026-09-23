package com.abrarshakhi.bricks.core.data.system

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LauncherAccessibilityService : AccessibilityService(), KoinComponent {

    private val bridge: AccessibilityBridge by inject()

    override fun onServiceConnected() {
        super.onServiceConnected()
        bridge.attach(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bridge.detach(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        bridge.detach(this)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
