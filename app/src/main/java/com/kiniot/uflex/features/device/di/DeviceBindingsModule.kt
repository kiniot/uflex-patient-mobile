package com.kiniot.uflex.features.device.di

import com.kiniot.uflex.features.device.data.remote.datasource.DeviceRemoteDataSource
import com.kiniot.uflex.features.device.data.remote.datasource.DeviceRemoteDataSourceImpl
import com.kiniot.uflex.features.device.data.repository.DeviceRepositoryImpl
import com.kiniot.uflex.features.device.domain.repository.DeviceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceBindingsModule {
    @Binds
    abstract fun bindDeviceRemoteDataSource(
        impl: DeviceRemoteDataSourceImpl
    ): DeviceRemoteDataSource

    @Binds
    abstract fun bindDeviceRepository(
        impl: DeviceRepositoryImpl
    ): DeviceRepository
}
