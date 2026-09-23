package com.abrarshakhi.bricks.feature.widgets

import android.app.Activity
import android.content.Intent
import app.cash.turbine.test
import com.abrarshakhi.bricks.core.domain.model.HomeItem
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import com.abrarshakhi.bricks.core.domain.testing.FakeHomeLayoutRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeSettingsRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeWidgetProviderRepository
import com.abrarshakhi.bricks.core.domain.testing.MainDispatcherRule
import com.abrarshakhi.bricks.core.domain.testing.testWidgetProvider
import com.abrarshakhi.bricks.core.domain.usecase.home.AddWidgetToHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ObserveSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveWidgetProvidersUseCase
import com.abrarshakhi.bricks.core.ui.widget.ActivityResultBridge
import com.abrarshakhi.bricks.core.ui.widget.WidgetBinder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WidgetsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val weather = testWidgetProvider("weather")
    private val clock = testWidgetProvider("clock", configurable = true)
    private val layoutRepository = FakeHomeLayoutRepository()
    private val binder = FakeWidgetBinder()
    private val bridge = ActivityResultBridge()

    private fun viewModel(): WidgetsViewModel {
        val settingsRepository = FakeSettingsRepository()
        return WidgetsViewModel(
            observeProviders = ObserveWidgetProvidersUseCase(FakeWidgetProviderRepository(listOf(weather, clock))),
            observeSettings = ObserveSettingsUseCase(settingsRepository),
            addWidgetToHome = AddWidgetToHomeUseCase(layoutRepository, settingsRepository),
            widgetHost = binder,
            resultBridge = bridge,
        )
    }

    @Test
    fun `providers are grouped by app and filtered by the query`() {
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.loading)
        assertEquals(listOf("Clock", "Weather"), viewModel.state.value.groups.map { it.appLabel })

        viewModel.onIntent(WidgetsIntent.QueryChanged("wea"))

        assertEquals(listOf("Weather"), viewModel.state.value.groups.map { it.appLabel })
    }

    @Test
    fun `a bound widget without configuration is placed at once`() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onIntent(WidgetsIntent.Add(weather, CELL, CELL))
            assertEquals(WidgetsEffect.Added, awaitItem())
        }
        val widget = layoutRepository.state.value.items.single() as HomeItem.Widget
        assertEquals(weather.key, widget.provider)
        assertFalse(viewModel.state.value.adding)
    }

    @Test
    fun `a configurable widget is placed after its setup succeeds`() = runTest {
        binder.configurable += clock.key
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onIntent(WidgetsIntent.Add(clock, CELL, CELL))
            val configure = awaitItem() as WidgetsEffect.Configure
            assertTrue(layoutRepository.state.value.items.isEmpty())
            bridge.dispatch(configure.requestCode, Activity.RESULT_OK)
            assertEquals(WidgetsEffect.Added, awaitItem())
        }
        assertEquals(1, layoutRepository.state.value.items.size)
    }

    @Test
    fun `cancelling widget setup releases the id`() = runTest {
        binder.configurable += clock.key
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onIntent(WidgetsIntent.Add(clock, CELL, CELL))
            val configure = awaitItem() as WidgetsEffect.Configure
            bridge.dispatch(configure.requestCode, Activity.RESULT_CANCELED)
            assertEquals(WidgetsEffect.ShowMessage(WidgetsMessage.Cancelled), awaitItem())
        }
        assertEquals(listOf(idBoundTo(clock.key)), binder.deleted)
        assertTrue(layoutRepository.state.value.items.isEmpty())
    }

    @Test
    fun `refusing the bind permission cancels`() = runTest {
        binder.bindAllowed = false
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onIntent(WidgetsIntent.Add(weather, CELL, CELL))
            assertTrue(awaitItem() is WidgetsEffect.RequestBind)
            viewModel.onIntent(WidgetsIntent.BindResult(granted = false))
            assertEquals(WidgetsEffect.ShowMessage(WidgetsMessage.Cancelled), awaitItem())
        }
        assertEquals(1, binder.deleted.size)
        assertTrue(layoutRepository.state.value.items.isEmpty())
    }

    private fun idBoundTo(provider: WidgetProviderKey): Int = binder.boundTo.getValue(provider)

    private class FakeWidgetBinder : WidgetBinder {
        var bindAllowed = true
        val configurable = mutableSetOf<WidgetProviderKey>()
        val deleted = mutableListOf<Int>()
        val boundTo = mutableMapOf<WidgetProviderKey, Int>()
        private val providers = mutableMapOf<Int, WidgetProviderKey>()
        private var nextId = 1

        override fun allocateId(): Int = nextId++

        override fun deleteId(appWidgetId: Int) {
            deleted += appWidgetId
        }

        override fun allocatedIds(): Set<Int> = (1 until nextId).toSet() - deleted.toSet()

        override fun bindIfAllowed(appWidgetId: Int, provider: WidgetProviderKey): Boolean {
            boundTo[provider] = appWidgetId
            providers[appWidgetId] = provider
            return bindAllowed
        }

        override fun bindPermissionIntent(appWidgetId: Int, provider: WidgetProviderKey): Intent = Intent()

        override fun needsConfiguration(appWidgetId: Int): Boolean = providers[appWidgetId] in configurable
    }

    private companion object {
        const val CELL = 80f
    }
}
