package com.kiniot.uflex.features.device.di

import com.kiniot.uflex.features.device.data.remote.api.DeviceApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object DeviceApiModule {
    @Provides
    fun provideDeviceApiService(retrofit: Retrofit): DeviceApiService {
        return retrofit.create(DeviceApiService::class.java)
    }
}
