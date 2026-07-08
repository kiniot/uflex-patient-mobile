package com.kiniot.uflex.features.plan.di

import com.kiniot.uflex.features.plan.data.remote.datasource.ExerciseRemoteDataSource
import com.kiniot.uflex.features.plan.data.remote.datasource.ExerciseRemoteDataSourceImpl
import com.kiniot.uflex.features.plan.data.remote.datasource.PlanRemoteDataSource
import com.kiniot.uflex.features.plan.data.remote.datasource.PlanRemoteDataSourceImpl
import com.kiniot.uflex.features.plan.data.repository.ExerciseRepositoryImpl
import com.kiniot.uflex.features.plan.data.repository.PlanRepositoryImpl
import com.kiniot.uflex.features.plan.domain.repository.ExerciseRepository
import com.kiniot.uflex.features.plan.domain.repository.PlanRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlanBindingsModule {
    @Binds
    abstract fun bindPlanRemoteDataSource(impl: PlanRemoteDataSourceImpl): PlanRemoteDataSource

    @Binds
    abstract fun bindExerciseRemoteDataSource(
        impl: ExerciseRemoteDataSourceImpl
    ): ExerciseRemoteDataSource

    @Binds
    abstract fun bindPlanRepository(impl: PlanRepositoryImpl): PlanRepository

    @Binds
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository
}
