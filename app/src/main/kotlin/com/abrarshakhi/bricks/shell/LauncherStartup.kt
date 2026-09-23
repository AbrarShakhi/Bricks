package com.abrarshakhi.bricks.shell

import android.content.Context
import com.abrarshakhi.bricks.core.data.ApplicationScope
import com.abrarshakhi.bricks.core.domain.usecase.home.SynchronizeHomeLayoutUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.EnsureHomeGridUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LauncherStartup(
    private val context: Context,
    private val scope: ApplicationScope,
    private val ensureGrid: EnsureHomeGridUseCase,
    private val synchronize: SynchronizeHomeLayoutUseCase,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            val configuration = context.resources.configuration
            runCatching {
                ensureGrid(
                    smallestWidthDp = configuration.smallestScreenWidthDp,
                    largestWidthDp = maxOf(configuration.screenWidthDp, configuration.screenHeightDp),
                )
            }
            while (true) {
                try {
                    synchronize()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    delay(RETRY_DELAY_MILLIS)
                }
            }
        }
    }

    private companion object {
        const val RETRY_DELAY_MILLIS = 2_000L
    }
}
