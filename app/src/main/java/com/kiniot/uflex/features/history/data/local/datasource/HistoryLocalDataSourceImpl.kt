package com.kiniot.uflex.features.history.data.local.datasource

import com.kiniot.uflex.features.history.data.local.dao.HistorySessionDao
import com.kiniot.uflex.features.history.data.local.entity.HistorySessionEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class HistoryLocalDataSourceImpl @Inject constructor(
    private val dao: HistorySessionDao
) : HistoryLocalDataSource {
    override fun observeCompletedSessions(): Flow<List<HistorySessionEntity>> =
        dao.observeCompletedSessions()

    override suspend fun saveCompletedSession(session: HistorySessionEntity) {
        dao.upsert(session)
    }
}
