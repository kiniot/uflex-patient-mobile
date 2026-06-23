package com.kiniot.uflex.features.therapy.domain.usecase

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.therapy.domain.model.DailySchedule
import com.kiniot.uflex.features.therapy.domain.model.LiveRepEvent
import com.kiniot.uflex.features.therapy.domain.model.SessionProgress
import com.kiniot.uflex.features.therapy.domain.model.TherapySession
import com.kiniot.uflex.features.therapy.domain.repository.TherapyRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetDailyScheduleUseCase @Inject constructor(private val repository: TherapyRepository) {
    suspend operator fun invoke(): AppResult<DailySchedule> = repository.getDailySchedule()
}

class GetActiveSessionUseCase @Inject constructor(private val repository: TherapyRepository) {
    suspend operator fun invoke(): AppResult<TherapySession> = repository.getActiveSession()
}

class InitiateSessionUseCase @Inject constructor(private val repository: TherapyRepository) {
    suspend operator fun invoke(
        treatmentPlanId: String,
        routineId: String,
        iotDeviceId: String
    ): AppResult<TherapySession> = repository.initiate(treatmentPlanId, routineId, iotDeviceId)
}

class ConfirmHardwareUseCase @Inject constructor(private val repository: TherapyRepository) {
    suspend operator fun invoke(sessionId: String, sensorsPlaced: Boolean): AppResult<TherapySession> =
        repository.confirmHardware(sessionId, sensorsPlaced)
}

class StartSessionUseCase @Inject constructor(private val repository: TherapyRepository) {
    suspend operator fun invoke(sessionId: String): AppResult<TherapySession> =
        repository.startSession(sessionId)
}

class CancelSessionUseCase @Inject constructor(private val repository: TherapyRepository) {
    suspend operator fun invoke(sessionId: String, reason: String): AppResult<TherapySession> =
        repository.cancelSession(sessionId, reason)
}

class StartSerieUseCase @Inject constructor(private val repository: TherapyRepository) {
    suspend operator fun invoke(sessionId: String, serieId: String): AppResult<Unit> =
        repository.startSerie(sessionId, serieId)
}

class GetProgressUseCase @Inject constructor(private val repository: TherapyRepository) {
    suspend operator fun invoke(sessionId: String): AppResult<SessionProgress> =
        repository.getProgress(sessionId)
}

class ReportPainUseCase @Inject constructor(private val repository: TherapyRepository) {
    suspend operator fun invoke(sessionId: String, painLevel: Int): AppResult<Unit> =
        repository.reportPain(sessionId, painLevel)
}

class FinalizeSessionUseCase @Inject constructor(private val repository: TherapyRepository) {
    suspend operator fun invoke(sessionId: String): AppResult<TherapySession> =
        repository.finalize(sessionId)
}

class ObserveLiveProgressUseCase @Inject constructor(private val repository: TherapyRepository) {
    operator fun invoke(serialNumber: String): Flow<LiveRepEvent> =
        repository.observeLiveProgress(serialNumber)
}
