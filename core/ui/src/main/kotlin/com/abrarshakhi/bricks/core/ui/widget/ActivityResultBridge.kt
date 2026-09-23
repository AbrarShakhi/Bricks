package com.abrarshakhi.bricks.core.ui.widget

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

class ActivityResultBridge {
    private val pending = ConcurrentHashMap<Int, CompletableDeferred<Int>>()

    suspend fun await(requestCode: Int, launch: () -> Boolean): Int? {
        val deferred = CompletableDeferred<Int>()
        pending.put(requestCode, deferred)?.cancel()
        if (!launch()) {
            pending.remove(requestCode, deferred)
            return null
        }
        return try {
            deferred.await()
        } finally {
            pending.remove(requestCode, deferred)
        }
    }

    fun dispatch(requestCode: Int, resultCode: Int): Boolean =
        pending.remove(requestCode)?.complete(resultCode) ?: false
}
