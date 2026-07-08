package com.kiniot.uflex.features.plan.di

import com.kiniot.uflex.features.plan.data.remote.api.ExerciseApiService
import com.kiniot.uflex.features.plan.data.remote.api.TreatmentPlanApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object PlanApiModule {
    @Provides
    fun provideTreatmentPlanApiService(retrofit: Retrofit): TreatmentPlanApiService =
        retrofit.create(TreatmentPlanApiService::class.java)

    @Provides
    fun provideExerciseApiService(retrofit: Retrofit): ExerciseApiService =
        retrofit.create(ExerciseApiService::class.java)
}
