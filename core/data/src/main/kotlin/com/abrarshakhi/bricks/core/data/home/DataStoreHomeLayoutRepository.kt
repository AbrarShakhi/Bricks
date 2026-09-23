package com.abrarshakhi.bricks.core.data.home

import androidx.datastore.core.DataStore
import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import com.abrarshakhi.bricks.core.domain.repository.HomeLayoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class DataStoreHomeLayoutRepository(
    private val store: DataStore<HomeLayoutDto>,
) : HomeLayoutRepository {

    override val layout: Flow<HomeLayout> = store.data
        .map { it.toDomain() }
        .distinctUntilChanged()

    override suspend fun update(transform: (HomeLayout) -> HomeLayout): HomeLayout =
        store.updateData { dto ->
            val current = dto.toDomain()
            val updated = transform(current)
            if (updated == current) dto else updated.toDto()
        }.toDomain()
}
