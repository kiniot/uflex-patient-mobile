package com.kiniot.uflex.features.auth.di

import com.kiniot.uflex.features.auth.data.local.datasource.AuthLocalDataSource
import com.kiniot.uflex.features.auth.data.local.datasource.AuthLocalDataSourceImpl
import com.kiniot.uflex.features.auth.data.remote.datasource.AuthRemoteDataSource
import com.kiniot.uflex.features.auth.data.remote.datasource.AuthRemoteDataSourceImpl
import com.kiniot.uflex.features.auth.data.repository.AuthRepositoryImpl
import com.kiniot.uflex.features.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindingsModule {

    @Binds
    @Singleton
    abstract fun bindAuthLocalDataSource(
        authLocalDataSourceImpl: AuthLocalDataSourceImpl
    ): AuthLocalDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        authRemoteDataSourceImpl: AuthRemoteDataSourceImpl
    ): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}
