package com.kiniot.uflex.features.therapy.data.remote.api

import com.kiniot.uflex.features.therapy.data.remote.dto.CancelSessionRequestDto
import com.kiniot.uflex.features.therapy.data.remote.dto.ConfirmHardwareRequestDto
import com.kiniot.uflex.features.therapy.data.remote.dto.DailyScheduleResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.InitiateSessionRequestDto
import com.kiniot.uflex.features.therapy.data.remote.dto.TherapySessionResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TherapyApiService {
    @GET("therapy-sessions/schedule/{patientId}")
    suspend fun getDailySchedule(
        @Path("patientId") patientId: String,
        @Query("date") date: String? = null
    ): Response<DailyScheduleResponseDto>

    @GET("therapy-sessions/active/{patientId}")
    suspend fun getActiveSession(@Path("patientId") patientId: String): Response<TherapySessionResponseDto>

    @POST("therapy-sessions")
    suspend fun initiate(@Body request: InitiateSessionRequestDto): Response<TherapySessionResponseDto>

    @PATCH("therapy-sessions/{id}/hardware")
    suspend fun confirmHardware(
        @Path("id") sessionId: String,
        @Body request: ConfirmHardwareRequestDto
    ): Response<TherapySessionResponseDto>

    @PATCH("therapy-sessions/{id}/start")
    suspend fun start(@Path("id") sessionId: String): Response<TherapySessionResponseDto>

    @PATCH("therapy-sessions/{id}/cancel")
    suspend fun cancel(
        @Path("id") sessionId: String,
        @Body request: CancelSessionRequestDto
    ): Response<TherapySessionResponseDto>
}
