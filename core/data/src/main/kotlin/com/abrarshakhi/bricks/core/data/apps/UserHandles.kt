package com.abrarshakhi.bricks.core.data.apps

import android.graphics.Rect
import android.os.UserHandle
import android.os.UserManager
import com.abrarshakhi.bricks.core.domain.model.ScreenBounds

internal fun UserManager.userFor(serial: Long): UserHandle? =
    runCatching { getUserForSerialNumber(serial) }.getOrNull()

internal fun ScreenBounds.toRect(): Rect = Rect(left, top, right, bottom)
