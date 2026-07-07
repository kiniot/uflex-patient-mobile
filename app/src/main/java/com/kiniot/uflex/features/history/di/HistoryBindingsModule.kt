package com.kiniot.uflex.features.history.di

import com.kiniot.uflex.features.history.data.local.datasource.HistoryLocalDataSource
import com.kiniot.uflex.features.history.data.local.datasource.HistoryLocalDataSourceImpl
import com.kiniot.uflex.features.history.data.repository.HistoryRepositoryImpl
import com.kiniot.uflex.features.history.domain.repository.HistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryBindingsModule {
    @Binds
    abstract fun bindHistoryLocalDataSource(
        impl: HistoryLocalDataSourceImpl
    ): HistoryLocalDataSource

    @Binds
    abstract fun bindHistoryRepository(
        impl: HistoryRepositoryImpl
    ): HistoryRepository
}
