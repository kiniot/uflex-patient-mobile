package com.kiniot.uflex.features.history.data.local.datasource

import com.kiniot.uflex.features.history.data.local.entity.HistorySessionEntity
import kotlinx.coroutines.flow.Flow

interface HistoryLocalDataSource {
    fun observeCompletedSessions(): Flow<List<HistorySessionEntity>>
    suspend fun saveCompletedSession(session: HistorySessionEntity)
}
