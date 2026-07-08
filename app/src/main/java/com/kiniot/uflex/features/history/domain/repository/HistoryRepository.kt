package com.kiniot.uflex.features.history.domain.repository

import com.kiniot.uflex.features.history.domain.model.HistorySession
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeCompletedSessions(): Flow<List<HistorySession>>
    suspend fun saveCompletedSession(session: HistorySession)
}
