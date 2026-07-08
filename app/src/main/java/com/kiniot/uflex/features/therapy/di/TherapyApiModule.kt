package com.kiniot.uflex.features.therapy.di

import com.kiniot.uflex.features.therapy.data.remote.api.TherapyApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object TherapyApiModule {
    @Provides
    fun provideTherapyApiService(retrofit: Retrofit): TherapyApiService =
        retrofit.create(TherapyApiService::class.java)
}
