package com.kiniot.uflex.features.therapy.data.mapper

import com.kiniot.uflex.features.therapy.data.remote.dto.DailyScheduleResponseDto
import com.kiniot.uflex.features.therapy.data.remote.dto.TherapySessionResponseDto
import com.kiniot.uflex.features.therapy.domain.model.DailySchedule
import com.kiniot.uflex.features.therapy.domain.model.ScheduleResolution
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

private fun String?.toSessionStatus(): SessionStatus = when (this) {
    "Pending" -> SessionStatus.Pending
    "Ready" -> SessionStatus.Ready
    "InProgress" -> SessionStatus.InProgress
    "Completed" -> SessionStatus.Completed
    "Cancelled" -> SessionStatus.Cancelled
    else -> SessionStatus.Unknown
}

private fun String?.toScheduleResolution(): ScheduleResolution = when (this) {
    "FOUND" -> ScheduleResolution.Found
    "NO_ACTIVE_PLAN_FOR_DATE" -> ScheduleResolution.NoActivePlan
    "NO_ROUTINE_FOR_DAY" -> ScheduleResolution.NoRoutineForDay
    else -> ScheduleResolution.Unknown
}
