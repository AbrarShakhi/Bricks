package com.abrarshakhi.bricks.core.data.settings

import androidx.datastore.core.DataStore
import com.abrarshakhi.bricks.core.domain.model.LauncherSettings
import com.abrarshakhi.bricks.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class DataStoreSettingsRepository(
    private val store: DataStore<SettingsDto>,
) : SettingsRepository {

    override val settings: Flow<LauncherSettings> = store.data
        .map { it.toDomain() }
        .distinctUntilChanged()

    override suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        store.updateData { dto -> transform(dto.toDomain()).toDto() }
    }
}
