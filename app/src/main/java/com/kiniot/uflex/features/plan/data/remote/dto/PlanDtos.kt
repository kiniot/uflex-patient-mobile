package com.kiniot.uflex.features.plan.data.remote.dto

import com.kiniot.uflex.core.serializers.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class TreatmentPlanResponseDto(
    val id: String,
    val patientId: String? = null,
    val name: String,
    val status: String,
    val period: PlanPeriodDto? = null,
    val routines: List<RoutineDto> = emptyList()
)

@Serializable
data class PlanPeriodDto(
    @Serializable(with = LocalDateSerializer::class) val startsAt: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class) val endsAt: LocalDate? = null
)

@Serializable
data class RoutineDto(
    val id: String,
    val name: String,
    val order: Int? = null,
    val schedule: RoutineScheduleDto? = null,
    val exerciseSeries: List<ExerciseSeriesDto> = emptyList()
)

@Serializable
data class RoutineScheduleDto(
    val dayOfWeek: String? = null,
    val scheduledTime: String? = null
)

@Serializable
data class ExerciseSeriesDto(
    val order: Int? = null,
    val exerciseId: String,
    val rangeOfMotionDegrees: Int? = null,
    val repetitions: Int? = null,
    val durationSeconds: Int? = null,
    val restDurationSeconds: Int? = null
)
