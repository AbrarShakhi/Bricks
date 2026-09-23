package com.abrarshakhi.bricks.core.domain.repository

import com.abrarshakhi.bricks.core.domain.model.HomeLayout
import kotlinx.coroutines.flow.Flow

interface HomeLayoutRepository {
    val layout: Flow<HomeLayout>

    suspend fun update(transform: (HomeLayout) -> HomeLayout): HomeLayout
}
