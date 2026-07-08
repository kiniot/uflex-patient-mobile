package com.kiniot.uflex.features.plan.domain.usecase

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.plan.domain.model.Exercise
import com.kiniot.uflex.features.plan.domain.repository.ExerciseRepository
import javax.inject.Inject

class GetExerciseDetailUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    suspend operator fun invoke(exerciseId: String): AppResult<Exercise> =
        repository.getExercise(exerciseId)
}
