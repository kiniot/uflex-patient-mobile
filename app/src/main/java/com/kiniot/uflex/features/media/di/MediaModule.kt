package com.kiniot.uflex.features.media.di

import com.kiniot.uflex.core.network.StorageHttpClient
import com.kiniot.uflex.features.media.data.remote.api.MediaApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideMediaApiService(retrofit: Retrofit): MediaApiService =
        retrofit.create(MediaApiService::class.java)

    /**
     * OkHttp client for direct uploads to Supabase Storage. No AuthInterceptor
     * (so our JWT is not leaked) and a long write timeout to tolerate large videos.
     */
    @Provides
    @Singleton
    @StorageHttpClient
    fun provideStorageOkHttp(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
}
