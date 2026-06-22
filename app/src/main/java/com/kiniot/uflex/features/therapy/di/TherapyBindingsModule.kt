package com.kiniot.uflex.features.therapy.di

import com.kiniot.uflex.features.therapy.data.remote.datasource.TherapyRemoteDataSource
import com.kiniot.uflex.features.therapy.data.remote.datasource.TherapyRemoteDataSourceImpl
import com.kiniot.uflex.features.therapy.data.repository.TherapyRepositoryImpl
import com.kiniot.uflex.features.therapy.domain.repository.TherapyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TherapyBindingsModule {
    @Binds
    abstract fun bindTherapyRemoteDataSource(
        impl: TherapyRemoteDataSourceImpl
    ): TherapyRemoteDataSource

    @Binds
    abstract fun bindTherapyRepository(impl: TherapyRepositoryImpl): TherapyRepository
}
