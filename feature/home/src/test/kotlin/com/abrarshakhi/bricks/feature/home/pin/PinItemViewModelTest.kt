package com.abrarshakhi.bricks.feature.home.pin

import android.content.Intent
import app.cash.turbine.test
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.HomeItem
import com.abrarshakhi.bricks.core.domain.model.Placement
import com.abrarshakhi.bricks.core.domain.model.ShortcutKey
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import com.abrarshakhi.bricks.core.domain.testing.FakeHomeLayoutRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeSettingsRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeShortcutRepository
import com.abrarshakhi.bricks.core.domain.testing.FakeWidgetProviderRepository
import com.abrarshakhi.bricks.core.domain.testing.MainDispatcherRule
import com.abrarshakhi.bricks.core.domain.testing.testWidgetProvider
import com.abrarshakhi.bricks.core.domain.usecase.home.AddWidgetToHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.FindWidgetProviderUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.PinShortcutToHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ObserveSettingsUseCase
import com.abrarshakhi.bricks.core.ui.widget.WidgetBinder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PinItemViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val weather = testWidgetProvider("weather")
    private val layoutRepository = FakeHomeLayoutRepository()
    private val settingsRepository = FakeSettingsRepository()
    private val shortcutRepository = FakeShortcutRepository()
    private val binder = FakeWidgetBinder()

    private fun viewModel(request: FakePinRequest) = PinItemViewModel(
        request = request,
        observeSettings = ObserveSettingsUseCase(settingsRepository),
        findWidgetProvider = FindWidgetProviderUseCase(FakeWidgetProviderRepository(listOf(weather))),
        pinShortcut = PinShortcutToHomeUseCase(layoutRepository, settingsRepository, shortcutRepository),
        addWidgetToHome = AddWidgetToHomeUseCase(layoutRepository, settingsRepository),
        widgetHost = binder,
    )

    @Test
    fun `a missing request finishes as failed`() = runTest {
        val viewModel = viewModel(FakePinRequest(candidate = null))

        viewModel.effects.test {
            assertEquals(PinItemEffect.Finish(PinOutcome.Failed), awaitItem())
        }
    }

    @Test
    fun `accepted shortcut lands on the home screen without pinning again`() = runTest {
        val request = FakePinRequest(SHORTCUT)
        val viewModel = viewModel(request)

        viewModel.effects.test {
            viewModel.onIntent(PinItemIntent.Confirm(CELL_WIDTH, CELL_HEIGHT))
            assertEquals(PinItemEffect.Finish(PinOutcome.Added), awaitItem())
        }
        assertTrue(request.shortcutAccepted)
        val placed = layoutRepository.state.value.items.single() as HomeItem.Shortcut
        assertEquals(SHORTCUT.key, placed.shortcut)
        assertEquals(SHORTCUT.label, placed.label)
        assertTrue(shortcutRepository.pinned.isEmpty())
    }

    @Test
    fun `a shortcut the system refuses is not placed`() = runTest {
        val viewModel = viewModel(FakePinRequest(SHORTCUT, acceptResult = false))

        viewModel.effects.test {
            viewModel.onIntent(PinItemIntent.Confirm(CELL_WIDTH, CELL_HEIGHT))
            assertEquals(PinItemEffect.Finish(PinOutcome.Failed), awaitItem())
        }
        assertTrue(layoutRepository.state.value.items.isEmpty())
    }

    @Test
    fun `bound widget is accepted and placed with its default span`() = runTest {
        val request = FakePinRequest(widgetCandidate())
        val viewModel = viewModel(request)

        viewModel.effects.test {
            viewModel.onIntent(PinItemIntent.Confirm(CELL_WIDTH, CELL_HEIGHT))
            assertEquals(PinItemEffect.Finish(PinOutcome.Added), awaitItem())
        }
        assertEquals(FakeWidgetBinder.FIRST_ID, request.acceptedWidgetId)
        val placed = layoutRepository.state.value.items.single() as HomeItem.Widget
        assertEquals(FakeWidgetBinder.FIRST_ID, placed.appWidgetId)
        assertEquals(weather.key, placed.provider)
        assertEquals(GridSpan(2, 2), (placed.placement as Placement.Desktop).span)
        assertTrue(binder.deleted.isEmpty())
    }

    @Test
    fun `refusing the bind permission releases the widget id`() = runTest {
        binder.bindAllowed = false
        val request = FakePinRequest(widgetCandidate())
        val viewModel = viewModel(request)

        viewModel.effects.test {
            viewModel.onIntent(PinItemIntent.Confirm(CELL_WIDTH, CELL_HEIGHT))
            assertTrue(awaitItem() is PinItemEffect.RequestBind)
            viewModel.onIntent(PinItemIntent.BindResult(granted = false))
            assertEquals(PinItemEffect.Finish(PinOutcome.Cancelled), awaitItem())
        }
        assertEquals(listOf(FakeWidgetBinder.FIRST_ID), binder.deleted)
        assertEquals(null, request.acceptedWidgetId)
        assertTrue(layoutRepository.state.value.items.isEmpty())
    }

    @Test
    fun `granting the bind permission completes the pin`() = runTest {
        binder.bindAllowed = false
        val request = FakePinRequest(widgetCandidate())
        val viewModel = viewModel(request)

        viewModel.effects.test {
            viewModel.onIntent(PinItemIntent.Confirm(CELL_WIDTH, CELL_HEIGHT))
            assertTrue(awaitItem() is PinItemEffect.RequestBind)
            viewModel.onIntent(PinItemIntent.BindResult(granted = true))
            assertEquals(PinItemEffect.Finish(PinOutcome.Added), awaitItem())
        }
        assertEquals(FakeWidgetBinder.FIRST_ID, request.acceptedWidgetId)
        assertEquals(1, layoutRepository.state.value.items.size)
    }

    @Test
    fun `dismissing the dialog cancels`() = runTest {
        val viewModel = viewModel(FakePinRequest(SHORTCUT))

        viewModel.effects.test {
            viewModel.onIntent(PinItemIntent.Dismiss)
            assertEquals(PinItemEffect.Finish(PinOutcome.Cancelled), awaitItem())
        }
        assertTrue(layoutRepository.state.value.items.isEmpty())
    }

    private fun widgetCandidate() = PinCandidate.Widget(
        provider = weather.key,
        label = weather.label,
        appLabel = weather.appLabel,
    )

    private class FakePinRequest(
        override val candidate: PinCandidate?,
        private val acceptResult: Boolean = true,
    ) : PinItemRequestHandle {
        var shortcutAccepted = false
        var acceptedWidgetId: Int? = null

        override fun acceptShortcut(): Boolean {
            shortcutAccepted = acceptResult
            return acceptResult
        }

        override fun acceptWidget(appWidgetId: Int): Boolean {
            if (acceptResult) acceptedWidgetId = appWidgetId
            return acceptResult
        }
    }

    private class FakeWidgetBinder : WidgetBinder {
        var bindAllowed = true
        val deleted = mutableListOf<Int>()
        private val allocated = mutableSetOf<Int>()
        private var nextId = FIRST_ID

        override fun allocateId(): Int = nextId++.also { allocated += it }

        override fun deleteId(appWidgetId: Int) {
            deleted += appWidgetId
            allocated -= appWidgetId
        }

        override fun allocatedIds(): Set<Int> = allocated.toSet()

        override fun bindIfAllowed(appWidgetId: Int, provider: WidgetProviderKey): Boolean = bindAllowed

        override fun bindPermissionIntent(appWidgetId: Int, provider: WidgetProviderKey): Intent = Intent()

        override fun needsConfiguration(appWidgetId: Int): Boolean = false

        companion object {
            const val FIRST_ID = 41
        }
    }

    private companion object {
        const val CELL_WIDTH = 80f
        const val CELL_HEIGHT = 100f
        val SHORTCUT = PinCandidate.Shortcut(
            key = ShortcutKey("com.android.chrome", "site-1", 0L),
            label = "Example Domain",
            appLabel = "Chrome",
            icon = PinIconSource { null },
        )
    }
}
