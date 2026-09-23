package com.abrarshakhi.bricks.feature.widgets

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.WidgetProvider
import com.abrarshakhi.bricks.core.domain.search.AppSearchEngine
import com.abrarshakhi.bricks.core.domain.usecase.home.AddWidgetToHomeUseCase
import com.abrarshakhi.bricks.core.domain.usecase.settings.ObserveSettingsUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.ObserveWidgetProvidersUseCase
import com.abrarshakhi.bricks.core.domain.usecase.system.WidgetProviderGroup
import com.abrarshakhi.bricks.core.ui.mvi.MviViewModel
import com.abrarshakhi.bricks.core.ui.widget.ActivityResultBridge
import com.abrarshakhi.bricks.core.ui.widget.WidgetBinder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class WidgetsViewModel(
    observeProviders: ObserveWidgetProvidersUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val addWidgetToHome: AddWidgetToHomeUseCase,
    private val widgetHost: WidgetBinder,
    resultBridge: ActivityResultBridge,
) : MviViewModel<WidgetsState, WidgetsIntent, WidgetsEffect>(WidgetsState()) {

    private var allGroups: List<WidgetProviderGroup> = emptyList()
    private var pending: Pending? = null

    init {
        viewModelScope.launch {
            combine(observeProviders(), observeSettings()) { groups, settings -> groups to (settings.home.grid ?: GridSize.of(4, 5)) }
                .collect { (groups, grid) ->
                    allGroups = groups
                    reduce { copy(loading = false, grid = grid, groups = filter(groups, query)) }
                }
        }
        viewModelScope.launch {
            resultBridge.results
                .filter { it.requestCode == CONFIGURE_REQUEST_CODE }
                .collect { event -> onConfigured(event.resultCode == Activity.RESULT_OK) }
        }
    }

    override fun onIntent(intent: WidgetsIntent) {
        when (intent) {
            is WidgetsIntent.QueryChanged -> reduce { copy(query = intent.query, groups = filter(allGroups, intent.query)) }
            is WidgetsIntent.Add -> startAdding(intent.provider, intent.cellWidthDp, intent.cellHeightDp)
            is WidgetsIntent.BindResult -> if (intent.granted) configureOrPlace() else fail(WidgetsMessage.Cancelled)
        }
    }

    private fun startAdding(provider: WidgetProvider, cellWidthDp: Float, cellHeightDp: Float) {
        if (pending != null) return
        val appWidgetId = runCatching { widgetHost.allocateId() }.getOrNull()
        if (appWidgetId == null) {
            emit(WidgetsEffect.ShowMessage(WidgetsMessage.Failed))
            return
        }
        val span = provider.defaultSpan(cellWidthDp, cellHeightDp, currentState.grid)
        pending = Pending(appWidgetId, provider, span)
        reduce { copy(adding = true) }
        if (widgetHost.bindIfAllowed(appWidgetId, provider.key)) {
            configureOrPlace()
        } else {
            val bindIntent = widgetHost.bindPermissionIntent(appWidgetId, provider.key)
            if (bindIntent == null) fail(WidgetsMessage.Failed) else emit(WidgetsEffect.RequestBind(bindIntent))
        }
    }

    private fun configureOrPlace() {
        val current = pending ?: return
        if (widgetHost.needsConfiguration(current.appWidgetId)) {
            emit(WidgetsEffect.Configure(current.appWidgetId, CONFIGURE_REQUEST_CODE))
        } else {
            place()
        }
    }

    private fun onConfigured(success: Boolean) {
        if (pending == null) return
        if (success) place() else fail(WidgetsMessage.Cancelled)
    }

    private fun place() {
        val current = pending ?: return
        viewModelScope.launch {
            addWidgetToHome(
                appWidgetId = current.appWidgetId,
                provider = current.provider.key,
                span = current.span,
                preferredPage = 0,
            )
            pending = null
            reduce { copy(adding = false) }
            emit(WidgetsEffect.Added)
        }
    }

    private fun fail(message: WidgetsMessage) {
        pending?.let { widgetHost.deleteId(it.appWidgetId) }
        pending = null
        reduce { copy(adding = false) }
        emit(WidgetsEffect.ShowMessage(message))
    }

    override fun onCleared() {
        pending?.let { widgetHost.deleteId(it.appWidgetId) }
        pending = null
        super.onCleared()
    }

    private fun filter(groups: List<WidgetProviderGroup>, query: String): List<WidgetProviderGroup> {
        val needle = AppSearchEngine.normalize(query.trim())
        if (needle.isEmpty()) return groups
        return groups.mapNotNull { group ->
            if (AppSearchEngine.normalize(group.appLabel).contains(needle)) {
                group
            } else {
                val matches = group.providers.filter { AppSearchEngine.normalize(it.label).contains(needle) }
                if (matches.isEmpty()) null else group.copy(providers = matches)
            }
        }
    }

    private data class Pending(
        val appWidgetId: Int,
        val provider: WidgetProvider,
        val span: GridSpan,
    )

    companion object {
        const val CONFIGURE_REQUEST_CODE = 7302
    }
}
