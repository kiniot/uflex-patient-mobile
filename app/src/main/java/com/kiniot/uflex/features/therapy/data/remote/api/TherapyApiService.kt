package com.kiniot.uflex.features.therapy.data.remote.api

import com.kiniot.uflex.features.therapy.data.remote.dto.CancelSessionRequestDto
import com.kiniot.uflex.features.therapy.data.remote.dto.ConfirmHardwareRequestDto
import com.kiniot.uflex.features.therapy.data.remote.dto.DailyScheduleResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.EdgeConnectionResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.InitiateSessionRequestDto
import com.kiniot.uflex.features.therapy.data.remote.dto.ReportPainRequestDto
import com.kiniot.uflex.features.therapy.data.remote.dto.SessionProgressResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.SessionSummaryResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.TherapySessionResponseDto
import okhttp3.ResponseBody
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

    // --- Phase 3: execution ---

    /** Start a serie (Pending -> Started). Body (SerieDetails) is ignored; we re-poll progress. */
    @PATCH("therapy-sessions/{id}/series/{serieId}/start")
    suspend fun startSerie(
        @Path("id") sessionId: String,
        @Path("serieId") serieId: String
    ): Response<ResponseBody>

    @GET("therapy-sessions/{id}/progress")
    suspend fun getProgress(@Path("id") sessionId: String): Response<SessionProgressResponseDto>

    @GET("therapy-sessions/{id}/summary")
    suspend fun getSessionSummary(@Path("id") sessionId: String): Response<SessionSummaryResponseDto>

    /** Report a pain level (0-10). Backend returns an empty 200. */
    @PATCH("therapy-sessions/{id}/pain")
    suspend fun reportPain(
        @Path("id") sessionId: String,
        @Body request: ReportPainRequestDto
    ): Response<ResponseBody>

    @PATCH("therapy-sessions/{id}/finalize")
    suspend fun finalize(@Path("id") sessionId: String): Response<TherapySessionResponseDto>

    /** Rendezvous: the edge's LAN URL + the pairing token to authenticate the live SSE stream. */
    @GET("patients/me/edge-connection")
    suspend fun getEdgeConnection(): Response<EdgeConnectionResponseDto>
}
