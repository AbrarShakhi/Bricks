package com.abrarshakhi.bricks

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.abrarshakhi.bricks.di.appModules
import com.abrarshakhi.bricks.feature.home.pin.PinItemRequestHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

class KoinGraphTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `every definition can be resolved`() {
        module { includes(appModules) }.verify(
            extraTypes = listOf(
                Context::class,
                Application::class,
                SavedStateHandle::class,
                CreationExtras::class,
                PinItemRequestHandle::class,
            ),
        )
    }
}
