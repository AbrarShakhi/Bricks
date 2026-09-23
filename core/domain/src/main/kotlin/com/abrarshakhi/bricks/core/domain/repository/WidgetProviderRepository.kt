package com.abrarshakhi.bricks.core.domain.repository

import com.abrarshakhi.bricks.core.domain.model.WidgetProvider
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import kotlinx.coroutines.flow.Flow

interface WidgetProviderRepository {
    val providers: Flow<List<WidgetProvider>>

    suspend fun find(key: WidgetProviderKey): WidgetProvider?
}
