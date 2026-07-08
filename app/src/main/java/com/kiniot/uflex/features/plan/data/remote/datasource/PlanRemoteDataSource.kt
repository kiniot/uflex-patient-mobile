package com.kiniot.uflex.features.plan.data.remote.datasource

import com.kiniot.uflex.core.network.SafeApiCaller
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.plan.data.remote.api.ExerciseApiService
import com.kiniot.uflex.features.plan.data.remote.api.TreatmentPlanApiService
import com.kiniot.uflex.features.plan.data.remote.dto.ExerciseResponseDto
import com.kiniot.uflex.features.plan.data.remote.dto.TreatmentPlanResponseDto
import javax.inject.Inject

interface PlanRemoteDataSource {
    suspend fun getActivePlan(): AppResult<TreatmentPlanResponseDto>
    suspend fun getScheduledPlans(): AppResult<List<TreatmentPlanResponseDto>>
}

interface ExerciseRemoteDataSource {
    suspend fun getExercises(): AppResult<List<ExerciseResponseDto>>
    suspend fun getExercise(id: String): AppResult<ExerciseResponseDto>
}

class PlanRemoteDataSourceImpl @Inject constructor(
    private val apiService: TreatmentPlanApiService,
    private val safeApiCaller: SafeApiCaller
) : PlanRemoteDataSource {
    override suspend fun getActivePlan(): AppResult<TreatmentPlanResponseDto> =
        safeApiCaller.execute { apiService.getActivePlan() }

    override suspend fun getScheduledPlans(): AppResult<List<TreatmentPlanResponseDto>> =
        safeApiCaller.execute { apiService.getScheduledPlans() }
}

class ExerciseRemoteDataSourceImpl @Inject constructor(
    private val apiService: ExerciseApiService,
    private val safeApiCaller: SafeApiCaller
) : ExerciseRemoteDataSource {
    override suspend fun getExercises(): AppResult<List<ExerciseResponseDto>> =
        safeApiCaller.execute { apiService.getExercises() }

    override suspend fun getExercise(id: String): AppResult<ExerciseResponseDto> =
        safeApiCaller.execute { apiService.getExercise(id) }
}
