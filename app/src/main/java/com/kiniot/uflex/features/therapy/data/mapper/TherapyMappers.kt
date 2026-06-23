package com.kiniot.uflex.features.therapy.data.mapper

import com.kiniot.uflex.features.therapy.data.remote.dto.DailyScheduleResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.EdgeConnectionResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.LiveRepEventDto
import com.kiniot.uflex.features.therapy.data.remote.dto.SerieProgressResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.SessionProgressResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.TherapySessionResponseDto
import com.kiniot.uflex.features.therapy.domain.model.DailySchedule
import com.kiniot.uflex.features.therapy.domain.model.EdgeConnection
import com.kiniot.uflex.features.therapy.domain.model.LiveRepEvent
import com.kiniot.uflex.features.therapy.domain.model.ScheduleResolution
import com.kiniot.uflex.features.therapy.domain.model.SerieProgress
import com.kiniot.uflex.features.therapy.domain.model.SerieStatus
import com.kiniot.uflex.features.therapy.domain.model.SessionProgress
import com.kiniot.uflex.features.therapy.domain.model.SessionStatus
import com.kiniot.uflex.features.therapy.domain.model.TherapySession

fun TherapySessionResponseDto.toDomain(): TherapySession = TherapySession(
    id = id,
    status = status.toSessionStatus(),
    sensorsPlaced = sensorsPlaced
)

fun DailyScheduleResponseDto.toDomain(): DailySchedule = DailySchedule(
    resolution = resolutionStatus.toScheduleResolution(),
    routineId = routineId,
    totalSeries = totalSeries ?: 0,
    estimatedDurationMinutes = estimatedDurationMinutes ?: 0
)

fun SessionProgressResponseDto.toDomain(): SessionProgress = SessionProgress(
    sessionId = sessionId.orEmpty(),
    status = status.toSessionStatus(),
    currentSerieId = currentSerieId,
    completedSeries = completedSeries ?: 0,
    totalSeries = totalSeries ?: 0,
    painLevel = painLevel,
    requiresClinicalReview = requiresClinicalReview ?: false,
    series = seriesProgress.map { it.toDomain() }
)

fun SerieProgressResponseDto.toDomain(): SerieProgress = SerieProgress(
    serieId = serieId.orEmpty(),
    exerciseId = exerciseId,
    currentRepetitions = currentRepetitions ?: 0,
    targetRepetitions = targetRepetitions ?: 0,
    status = status.toSerieStatus()
)

fun LiveRepEventDto.toDomain(): LiveRepEvent = LiveRepEvent(
    serieId = serieId,
    repsDetected = repsDetected,
    classification = classification
)

fun EdgeConnectionResponseDto.toDomain(): EdgeConnection = EdgeConnection(
    localEdgeUrl = localEdgeUrl,
    pairingToken = pairingToken,
    expiresAt = expiresAt
)

private fun String?.toSessionStatus(): SessionStatus = when (this) {
    "Pending" -> SessionStatus.Pending
    "Ready" -> SessionStatus.Ready
    "InProgress" -> SessionStatus.InProgress
    "Completed" -> SessionStatus.Completed
    "Cancelled" -> SessionStatus.Cancelled
    else -> SessionStatus.Unknown
}

private fun String?.toSerieStatus(): SerieStatus = when (this) {
    "Pending" -> SerieStatus.Pending
    "Started" -> SerieStatus.Started
    "Completed" -> SerieStatus.Completed
    "Failed" -> SerieStatus.Failed
    else -> SerieStatus.Unknown
}

private fun String?.toScheduleResolution(): ScheduleResolution = when (this) {
    "FOUND" -> ScheduleResolution.Found
    "NO_ACTIVE_PLAN_FOR_DATE" -> ScheduleResolution.NoActivePlan
    "NO_ROUTINE_FOR_DAY" -> ScheduleResolution.NoRoutineForDay
    else -> ScheduleResolution.Unknown
}
