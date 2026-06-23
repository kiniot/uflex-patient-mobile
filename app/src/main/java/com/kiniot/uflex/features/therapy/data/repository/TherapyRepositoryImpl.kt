package com.kiniot.uflex.features.therapy.data.repository

import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.core.session.SessionStore
import com.kiniot.uflex.features.therapy.data.mapper.toDomain
import com.kiniot.uflex.features.therapy.data.remote.datasource.EdgeProgressDataSource
import com.kiniot.uflex.features.therapy.data.remote.datasource.TherapyRemoteDataSource
import com.kiniot.uflex.features.therapy.data.remote.dto.InitiateSessionRequestDto
import com.kiniot.uflex.features.therapy.domain.model.DailySchedule
import com.kiniot.uflex.features.therapy.domain.model.LiveRepEvent
import com.kiniot.uflex.features.therapy.domain.model.SessionProgress
import com.kiniot.uflex.features.therapy.domain.model.TherapySession
import com.kiniot.uflex.features.therapy.domain.repository.TherapyRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class TherapyRepositoryImpl @Inject constructor(
    private val remoteDataSource: TherapyRemoteDataSource,
    private val edgeProgressDataSource: EdgeProgressDataSource,
    private val sessionStore: SessionStore
) : TherapyRepository {

    override suspend fun getDailySchedule(): AppResult<DailySchedule> {
        val patientId = currentPatientId() ?: return missingPatient()
        return when (val result = remoteDataSource.getDailySchedule(patientId)) {
            is AppResult.Success -> AppResult.Success(result.data.toDomain())
            is AppResult.Error -> result
        }
    }

    override suspend fun getActiveSession(): AppResult<TherapySession> {
        val patientId = currentPatientId() ?: return missingPatient()
        return when (val result = remoteDataSource.getActiveSession(patientId)) {
            is AppResult.Success -> AppResult.Success(result.data.toDomain())
            // "No active session" comes back as a domain-coded 404 (THERAPY_SESSION_NOT_FOUND),
            // which the core mapper turns into AppError.Business. Normalize it to NotFound so
            // callers can treat it as the empty/no-session state (e.g. preparation -> summary).
            is AppResult.Error -> AppResult.Error(result.error.asNotFoundIf404())
        }
    }

    override suspend fun initiate(
        treatmentPlanId: String,
        routineId: String,
        iotDeviceId: String
    ): AppResult<TherapySession> {
        val patientId = currentPatientId() ?: return missingPatient()
        val request = InitiateSessionRequestDto(
            patientId = patientId,
            treatmentPlanId = treatmentPlanId,
            iotDeviceId = iotDeviceId,
            routineId = routineId
        )
        return remoteDataSource.initiate(request).toSession()
    }

    override suspend fun confirmHardware(sessionId: String, sensorsPlaced: Boolean): AppResult<TherapySession> =
        remoteDataSource.confirmHardware(sessionId, sensorsPlaced).toSession()

    override suspend fun startSession(sessionId: String): AppResult<TherapySession> =
        remoteDataSource.start(sessionId).toSession()

    override suspend fun cancelSession(sessionId: String, reason: String): AppResult<TherapySession> =
        remoteDataSource.cancel(sessionId, reason).toSession()

    override suspend fun startSerie(sessionId: String, serieId: String): AppResult<Unit> =
        remoteDataSource.startSerie(sessionId, serieId)

    override suspend fun getProgress(sessionId: String): AppResult<SessionProgress> =
        when (val result = remoteDataSource.getProgress(sessionId)) {
            is AppResult.Success -> AppResult.Success(result.data.toDomain())
            is AppResult.Error -> result
        }

    override suspend fun reportPain(sessionId: String, painLevel: Int): AppResult<Unit> =
        remoteDataSource.reportPain(sessionId, painLevel)

    override suspend fun finalize(sessionId: String): AppResult<TherapySession> =
        remoteDataSource.finalize(sessionId).toSession()

    override fun observeLiveProgress(serialNumber: String): Flow<LiveRepEvent> =
        edgeProgressDataSource.observeProgress(serialNumber)

    private suspend fun currentPatientId(): String? = sessionStore.getSession()?.patientId

    private fun missingPatient(): AppResult.Error = AppResult.Error(AppError.Unknown())

    /** A backend domain-coded 404 maps to [AppError.Business]; treat it as [AppError.NotFound]. */
    private fun AppError.asNotFoundIf404(): AppError =
        if (this is AppError.Business && status == 404) AppError.NotFound else this

    private fun AppResult<com.kiniot.uflex.features.therapy.data.remote.dto.TherapySessionResponseDto>.toSession():
        AppResult<TherapySession> = when (this) {
        is AppResult.Success -> AppResult.Success(data.toDomain())
        is AppResult.Error -> this
    }
}
