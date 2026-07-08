package com.kiniot.uflex.features.plan.data.remote.api

import com.kiniot.uflex.features.plan.data.remote.dto.ExerciseResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ExerciseApiService {
    @GET("exercises")
    suspend fun getExercises(): Response<List<ExerciseResponseDto>>

    @GET("exercises/{id}")
    suspend fun getExercise(@Path("id") id: String): Response<ExerciseResponseDto>
}
