package com.kiniot.uflex.features.therapy.data.remote.datasource

import com.kiniot.uflex.core.network.SafeApiCaller
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.therapy.data.remote.api.TherapyApiService
import com.kiniot.uflex.features.therapy.data.remote.dto.CancelSessionRequestDto
import com.kiniot.uflex.features.therapy.data.remote.dto.ConfirmHardwareRequestDto
import com.kiniot.uflex.features.therapy.data.remote.dto.DailyScheduleResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.EdgeConnectionResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.InitiateSessionRequestDto
import com.kiniot.uflex.features.therapy.data.remote.dto.ReportPainRequestDto
import com.kiniot.uflex.features.therapy.data.remote.dto.SessionProgressResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.SessionSummaryResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.TherapySessionResponseDto
import javax.inject.Inject

interface TherapyRemoteDataSource {
    suspend fun getDailySchedule(patientId: String): AppResult<DailyScheduleResponseDto>
    suspend fun getActiveSession(patientId: String): AppResult<TherapySessionResponseDto>
    suspend fun initiate(request: InitiateSessionRequestDto): AppResult<TherapySessionResponseDto>
    suspend fun confirmHardware(sessionId: String, sensorsPlaced: Boolean): AppResult<TherapySessionResponseDto>
    suspend fun start(sessionId: String): AppResult<TherapySessionResponseDto>
    suspend fun cancel(sessionId: String, reason: String): AppResult<TherapySessionResponseDto>
    suspend fun startSerie(sessionId: String, serieId: String): AppResult<Unit>
    suspend fun getProgress(sessionId: String): AppResult<SessionProgressResponseDto>
    suspend fun getSessionSummary(sessionId: String): AppResult<SessionSummaryResponseDto>
    suspend fun reportPain(sessionId: String, painLevel: Int): AppResult<Unit>
    suspend fun finalize(sessionId: String): AppResult<TherapySessionResponseDto>
    suspend fun getEdgeConnection(): AppResult<EdgeConnectionResponseDto>
}

class TherapyRemoteDataSourceImpl @Inject constructor(
    private val apiService: TherapyApiService,
    private val safeApiCaller: SafeApiCaller
) : TherapyRemoteDataSource {

    override suspend fun getDailySchedule(patientId: String): AppResult<DailyScheduleResponseDto> =
        safeApiCaller.execute { apiService.getDailySchedule(patientId) }

    override suspend fun getActiveSession(patientId: String): AppResult<TherapySessionResponseDto> =
        safeApiCaller.execute { apiService.getActiveSession(patientId) }

    override suspend fun initiate(request: InitiateSessionRequestDto): AppResult<TherapySessionResponseDto> =
        safeApiCaller.execute { apiService.initiate(request) }

    override suspend fun confirmHardware(
        sessionId: String,
        sensorsPlaced: Boolean
    ): AppResult<TherapySessionResponseDto> =
        safeApiCaller.execute { apiService.confirmHardware(sessionId, ConfirmHardwareRequestDto(sensorsPlaced)) }

    override suspend fun start(sessionId: String): AppResult<TherapySessionResponseDto> =
        safeApiCaller.execute { apiService.start(sessionId) }

    override suspend fun cancel(sessionId: String, reason: String): AppResult<TherapySessionResponseDto> =
        safeApiCaller.execute { apiService.cancel(sessionId, CancelSessionRequestDto(reason)) }

    override suspend fun startSerie(sessionId: String, serieId: String): AppResult<Unit> =
        safeApiCaller.executeNoContent { apiService.startSerie(sessionId, serieId) }

    override suspend fun getProgress(sessionId: String): AppResult<SessionProgressResponseDto> =
        safeApiCaller.execute { apiService.getProgress(sessionId) }

    override suspend fun getSessionSummary(sessionId: String): AppResult<SessionSummaryResponseDto> =
        safeApiCaller.execute { apiService.getSessionSummary(sessionId) }

    override suspend fun reportPain(sessionId: String, painLevel: Int): AppResult<Unit> =
        safeApiCaller.executeNoContent { apiService.reportPain(sessionId, ReportPainRequestDto(painLevel)) }

    override suspend fun finalize(sessionId: String): AppResult<TherapySessionResponseDto> =
        safeApiCaller.execute { apiService.finalize(sessionId) }

    override suspend fun getEdgeConnection(): AppResult<EdgeConnectionResponseDto> =
        safeApiCaller.execute { apiService.getEdgeConnection() }
}
