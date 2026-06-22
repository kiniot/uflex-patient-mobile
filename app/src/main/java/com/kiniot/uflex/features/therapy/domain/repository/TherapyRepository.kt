package com.kiniot.uflex.features.therapy.domain.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.therapy.domain.model.DailySchedule
import com.kiniot.uflex.features.therapy.domain.model.SessionProgress
import com.kiniot.uflex.features.therapy.domain.model.TherapySession

interface TherapyRepository {
    /** Today's scheduled routine for the current patient. */
    suspend fun getDailySchedule(): AppResult<DailySchedule>

    /** The current patient's active session, if any (NotFound when none). */
    suspend fun getActiveSession(): AppResult<TherapySession>

    suspend fun initiate(
        treatmentPlanId: String,
        routineId: String,
        iotDeviceId: String
    ): AppResult<TherapySession>

    suspend fun confirmHardware(sessionId: String, sensorsPlaced: Boolean): AppResult<TherapySession>

    suspend fun startSession(sessionId: String): AppResult<TherapySession>

    suspend fun cancelSession(sessionId: String, reason: String): AppResult<TherapySession>

    // --- Phase 3: execution ---

    suspend fun startSerie(sessionId: String, serieId: String): AppResult<Unit>

    suspend fun getProgress(sessionId: String): AppResult<SessionProgress>

    suspend fun reportPain(sessionId: String, painLevel: Int): AppResult<Unit>

    suspend fun finalize(sessionId: String): AppResult<TherapySession>
}
