package com.kiniot.uflex.features.history.data.repository

import com.kiniot.uflex.features.history.data.local.datasource.HistoryLocalDataSource
import com.kiniot.uflex.features.history.data.mapper.toDomain
import com.kiniot.uflex.features.history.data.mapper.toEntity
import com.kiniot.uflex.features.history.domain.model.HistorySession
import com.kiniot.uflex.features.history.domain.repository.HistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl @Inject constructor(
    private val localDataSource: HistoryLocalDataSource
) : HistoryRepository {
    override fun observeCompletedSessions(): Flow<List<HistorySession>> =
        localDataSource.observeCompletedSessions().map { sessions ->
            sessions.map { it.toDomain() }
        }

    override suspend fun saveCompletedSession(session: HistorySession) {
        localDataSource.saveCompletedSession(session.toEntity())
    }
}
