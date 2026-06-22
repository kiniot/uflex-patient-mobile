package com.kiniot.uflex.features.therapy.domain.model

/** Resolution of today's routine lookup. Mirrors the backend resolutionStatus values. */
enum class ScheduleResolution {
    Found,
    NoActivePlan,
    NoRoutineForDay,
    Unknown
}

/** The routine the patient is scheduled to perform on a given date (today by default). */
data class DailySchedule(
    val resolution: ScheduleResolution,
    val routineId: String?,
    val totalSeries: Int,
    val estimatedDurationMinutes: Int
)
