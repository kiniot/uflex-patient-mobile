package com.kiniot.uflex.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.kiniot.uflex.core.database.UFlexDatabase
import com.kiniot.uflex.features.history.data.local.dao.HistorySessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val SESSION_PREFERENCES_NAME = "uflex_session.preferences_pb"
private const val UFLEX_DATABASE_NAME = "uflex.db"

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(SESSION_PREFERENCES_NAME) }
        )
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): UFlexDatabase = Room.databaseBuilder(
        context,
        UFlexDatabase::class.java,
        UFLEX_DATABASE_NAME
    ).build()

    @Provides
    fun provideHistorySessionDao(database: UFlexDatabase): HistorySessionDao =
        database.historySessionDao()
}
