package com.kiniot.uflex.features.history.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kiniot.uflex.features.history.data.local.entity.HistorySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistorySessionDao {
    @Query("SELECT * FROM history_sessions ORDER BY COALESCE(finalizedAt, savedAt) DESC")
    fun observeCompletedSessions(): Flow<List<HistorySessionEntity>>

    @Upsert
    suspend fun upsert(session: HistorySessionEntity)
}
