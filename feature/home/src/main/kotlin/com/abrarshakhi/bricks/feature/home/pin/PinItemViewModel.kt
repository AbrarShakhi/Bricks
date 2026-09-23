package com.abrarshakhi.bricks.feature.home.pin

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import com.abrarshakhi.bricks.core.domain.usecase.home.AddWidgetToHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.FindWidgetProviderUseCase
import com.abrarshakhi.bricks.core.domain.usecase.home.PinShortcutToHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ObserveSettingsUseCase
import com.abrarshakhi.bricks.core.ui.mvi.MviViewModel
import com.abrarshakhi.bricks.core.ui.widget.LauncherWidgetHost
import kotlinx.coroutines.launch

class PinItemViewModel(
    private val request: PinItemRequestHandle,
    observeSettings: ObserveSettingsUseCase,
    private val findWidgetProvider: FindWidgetProviderUseCase,
    private val pinShortcut: PinShortcutToHomeUseCase,
    private val addWidgetToHome: AddWidgetToHomeUseCase,
    private val widgetHost: LauncherWidgetHost,
) : MviViewModel<PinItemState, PinItemIntent, PinItemEffect>(PinItemState(candidate = request.candidate)) {

    private var pending: PendingWidget? = null

    init {
        val candidate = request.candidate
        if (candidate == null) {
            emit(PinItemEffect.Finish(PinOutcome.Failed))
        } else {
            viewModelScope.launch {
                observeSettings().collect { settings ->
                    reduce { copy(appearance = settings.appearance, grid = settings.home.grid ?: PinItemState.DEFAULT_GRID) }
                }
            }
            if (candidate is PinCandidate.Widget) {
                viewModelScope.launch {
                    val provider = findWidgetProvider(candidate.provider)
                    if (provider == null) finish(PinOutcome.Failed) else reduce { copy(provider = provider) }
                }
            }
        }
    }

    override fun onIntent(intent: PinItemIntent) {
        when (intent) {
            is PinItemIntent.Confirm -> confirm(intent.cellWidthDp, intent.cellHeightDp)
            is PinItemIntent.BindResult -> if (intent.granted) acceptWidget() else finish(PinOutcome.Cancelled)
            PinItemIntent.Dismiss -> if (!currentState.busy || pending != null) finish(PinOutcome.Cancelled)
        }
    }

    private fun confirm(cellWidthDp: Float, cellHeightDp: Float) {
        val state = currentState
        if (!state.canConfirm) return
        when (val candidate = state.candidate) {
            is PinCandidate.Shortcut -> acceptShortcut(candidate)
            is PinCandidate.Widget -> {
                val provider = state.provider ?: return
                startWidget(provider.key, provider.defaultSpan(cellWidthDp, cellHeightDp, state.grid))
            }
            null -> Unit
        }
    }

    private fun acceptShortcut(candidate: PinCandidate.Shortcut) {
        reduce { copy(busy = true) }
        if (!request.acceptShortcut()) {
            finish(PinOutcome.Failed)
            return
        }
        viewModelScope.launch {
            pinShortcut(candidate.key, candidate.label, preferredPage = 0, alreadyPinned = true)
            emit(PinItemEffect.Finish(PinOutcome.Added))
        }
    }

    private fun startWidget(provider: WidgetProviderKey, span: GridSpan) {
        val appWidgetId = runCatching { widgetHost.allocateId() }.getOrNull()
        if (appWidgetId == null) {
            finish(PinOutcome.Failed)
            return
        }
        pending = PendingWidget(appWidgetId, provider, span)
        reduce { copy(busy = true) }
        if (widgetHost.bindIfAllowed(appWidgetId, provider)) {
            acceptWidget()
        } else {
            val bindIntent = widgetHost.bindPermissionIntent(appWidgetId, provider)
            if (bindIntent == null) finish(PinOutcome.Failed) else emit(PinItemEffect.RequestBind(bindIntent))
        }
    }

    private fun acceptWidget() {
        val widget = pending ?: return
        if (!request.acceptWidget(widget.appWidgetId)) {
            finish(PinOutcome.Failed)
            return
        }
        pending = null
        viewModelScope.launch {
            addWidgetToHome(
                appWidgetId = widget.appWidgetId,
                provider = widget.provider,
                span = widget.span,
                preferredPage = 0,
            )
            emit(PinItemEffect.Finish(PinOutcome.Added))
        }
    }

    private fun finish(outcome: PinOutcome) {
        releasePending()
        reduce { copy(busy = false) }
        emit(PinItemEffect.Finish(outcome))
    }

    private fun releasePending() {
        pending?.let { widgetHost.deleteId(it.appWidgetId) }
        pending = null
    }

    override fun onCleared() {
        releasePending()
        super.onCleared()
    }

    private data class PendingWidget(
        val appWidgetId: Int,
        val provider: WidgetProviderKey,
        val span: GridSpan,
    )
}
