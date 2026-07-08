package com.kiniot.uflex.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kiniot.uflex.features.history.data.local.dao.HistorySessionDao
import com.kiniot.uflex.features.history.data.local.entity.HistorySessionEntity

@Database(
    entities = [
        HistorySessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class UFlexDatabase : RoomDatabase() {
    abstract fun historySessionDao(): HistorySessionDao
}
