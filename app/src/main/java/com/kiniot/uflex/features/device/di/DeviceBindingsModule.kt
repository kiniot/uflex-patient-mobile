package com.kiniot.uflex.features.device.di

import com.kiniot.uflex.features.device.data.ble.DeviceBleDataSource
import com.kiniot.uflex.features.device.data.ble.DeviceBleDataSourceImpl
import com.kiniot.uflex.features.device.data.remote.datasource.DeviceRemoteDataSource
import com.kiniot.uflex.features.device.data.remote.datasource.DeviceRemoteDataSourceImpl
import com.kiniot.uflex.features.device.data.repository.DeviceConnectionRepositoryImpl
import com.kiniot.uflex.features.device.data.repository.DeviceRepositoryImpl
import com.kiniot.uflex.features.device.domain.repository.DeviceConnectionRepository
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

    @Binds
    abstract fun bindDeviceBleDataSource(
        impl: DeviceBleDataSourceImpl
    ): DeviceBleDataSource

    @Binds
    abstract fun bindDeviceConnectionRepository(
        impl: DeviceConnectionRepositoryImpl
    ): DeviceConnectionRepository
}
