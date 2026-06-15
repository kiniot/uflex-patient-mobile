package com.kiniot.uflex.features.profile.di

import com.kiniot.uflex.features.profile.data.local.datasource.ProfileLocalDataSource
import com.kiniot.uflex.features.profile.data.local.datasource.ProfileLocalDataSourceImpl
import com.kiniot.uflex.features.profile.data.remote.datasource.ProfileRemoteDataSource
import com.kiniot.uflex.features.profile.data.remote.datasource.ProfileRemoteDataSourceImpl
import com.kiniot.uflex.features.profile.data.repository.ProfileRepositoryImpl
import com.kiniot.uflex.features.profile.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileBindingsModule {
    @Binds
    abstract fun bindProfileLocalDataSource(
        impl: ProfileLocalDataSourceImpl
    ): ProfileLocalDataSource

    @Binds
    abstract fun bindProfileRemoteDataSource(
        impl: ProfileRemoteDataSourceImpl
    ): ProfileRemoteDataSource

    @Binds
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository
}
