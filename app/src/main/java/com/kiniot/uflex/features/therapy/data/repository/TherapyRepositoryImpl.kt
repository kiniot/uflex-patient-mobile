package com.kiniot.uflex.features.therapy.data.repository

import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.core.session.SessionStore
import com.kiniot.uflex.features.therapy.data.mapper.toDomain
import com.kiniot.uflex.features.therapy.data.remote.datasource.TherapyRemoteDataSource
import com.kiniot.uflex.features.therapy.data.remote.dto.InitiateSessionRequestDto
import com.kiniot.uflex.features.therapy.domain.model.DailySchedule
import com.kiniot.uflex.features.therapy.domain.model.TherapySession
import com.kiniot.uflex.features.therapy.domain.repository.TherapyRepository
import javax.inject.Inject

class TherapyRepositoryImpl @Inject constructor(
    private val remoteDataSource: TherapyRemoteDataSource,
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
        return remoteDataSource.getActiveSession(patientId).toSession()
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

    private suspend fun currentPatientId(): String? = sessionStore.getSession()?.patientId

    private fun missingPatient(): AppResult.Error = AppResult.Error(AppError.Unknown())

    private fun AppResult<com.kiniot.uflex.features.therapy.data.remote.dto.TherapySessionResponseDto>.toSession():
        AppResult<TherapySession> = when (this) {
        is AppResult.Success -> AppResult.Success(data.toDomain())
        is AppResult.Error -> this
    }
}
