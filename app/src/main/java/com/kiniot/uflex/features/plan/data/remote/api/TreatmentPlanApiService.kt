package com.kiniot.uflex.features.plan.data.remote.api

import com.kiniot.uflex.features.plan.data.remote.dto.TreatmentPlanResponseDto
import retrofit2.Response
import retrofit2.http.GET

interface TreatmentPlanApiService {
    @GET("patients/me/treatment-plans/active")
    suspend fun getActivePlan(): Response<TreatmentPlanResponseDto>

    @GET("patients/me/treatment-plans/scheduled")
    suspend fun getScheduledPlans(): Response<List<TreatmentPlanResponseDto>>
}
