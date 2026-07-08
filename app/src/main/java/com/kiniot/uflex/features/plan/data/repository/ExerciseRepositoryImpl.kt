package com.kiniot.uflex.features.plan.data.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.plan.data.mapper.toDomain
import com.kiniot.uflex.features.plan.data.remote.datasource.ExerciseRemoteDataSource
import com.kiniot.uflex.features.plan.domain.model.Exercise
import com.kiniot.uflex.features.plan.domain.repository.ExerciseRepository
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseRemoteDataSource: ExerciseRemoteDataSource
) : ExerciseRepository {

    override suspend fun getExercise(id: String): AppResult<Exercise> =
        when (val result = exerciseRemoteDataSource.getExercise(id)) {
            is AppResult.Success -> AppResult.Success(result.data.toDomain())
            is AppResult.Error -> result
        }
}
