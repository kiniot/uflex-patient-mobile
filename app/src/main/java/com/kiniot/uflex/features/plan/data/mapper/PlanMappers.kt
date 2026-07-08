package com.kiniot.uflex.features.plan.data.mapper

import com.kiniot.uflex.features.plan.data.remote.dto.ExerciseResponseDto
import com.kiniot.uflex.features.plan.data.remote.dto.ExerciseSeriesDto
import com.kiniot.uflex.features.plan.data.remote.dto.RoutineDto
import com.kiniot.uflex.features.plan.data.remote.dto.TreatmentPlanResponseDto
import com.kiniot.uflex.features.plan.domain.model.BodyPart
import com.kiniot.uflex.features.plan.domain.model.Exercise
import com.kiniot.uflex.features.plan.domain.model.MovementType
import com.kiniot.uflex.features.plan.domain.model.PlanExercise
import com.kiniot.uflex.features.plan.domain.model.PlanPeriod
import com.kiniot.uflex.features.plan.domain.model.PlanRoutine
import com.kiniot.uflex.features.plan.domain.model.PlanStatus
import com.kiniot.uflex.features.plan.domain.model.RoutineSchedule
import com.kiniot.uflex.features.plan.domain.model.TreatmentPlan

fun ExerciseResponseDto.toDomain(): Exercise = Exercise(
    id = id,
    name = name,
    description = description,
    bodyPart = bodyPart.toBodyPart(),
    movementType = movementType.toMovementType(),
    videoUrl = videoUrl
)

/**
 * Maps a plan DTO to domain, enriching each exercise series with its catalog identity
 * (name, body part, movement) from [catalog], keyed by exercise id.
 */
fun TreatmentPlanResponseDto.toDomain(catalog: Map<String, ExerciseResponseDto>): TreatmentPlan =
    TreatmentPlan(
        id = id,
        name = name,
        status = status.toPlanStatus(),
        period = period?.let { PlanPeriod(it.startsAt, it.endsAt) },
        routines = routines.sortedBy { it.order ?: 0 }.map { it.toDomain(catalog) }
    )

private fun RoutineDto.toDomain(catalog: Map<String, ExerciseResponseDto>): PlanRoutine = PlanRoutine(
    id = id,
    name = name,
    order = order ?: 0,
    schedule = schedule?.let { RoutineSchedule(it.dayOfWeek, it.scheduledTime) },
    exercises = exerciseSeries.sortedBy { it.order ?: 0 }.map { it.toPlanExercise(catalog) }
)

private fun ExerciseSeriesDto.toPlanExercise(catalog: Map<String, ExerciseResponseDto>): PlanExercise {
    val exercise = catalog[exerciseId]
    return PlanExercise(
        exerciseId = exerciseId,
        exerciseName = exercise?.name ?: exerciseId,
        bodyPart = exercise?.bodyPart.toBodyPart(),
        movementType = exercise?.movementType.toMovementType(),
        order = order ?: 0,
        rangeOfMotionDegrees = rangeOfMotionDegrees,
        repetitions = repetitions,
        durationSeconds = durationSeconds,
        restDurationSeconds = restDurationSeconds
    )
}

private fun String?.toBodyPart(): BodyPart = when (this) {
    "ELBOW" -> BodyPart.Elbow
    "WRIST" -> BodyPart.Wrist
    else -> BodyPart.Unknown
}

private fun String?.toMovementType(): MovementType = when (this) {
    "PRONATION" -> MovementType.Pronation
    "SUPINATION" -> MovementType.Supination
    "FLEXION" -> MovementType.Flexion
    "EXTENSION" -> MovementType.Extension
    else -> MovementType.Unknown
}

private fun String?.toPlanStatus(): PlanStatus = when (this) {
    "SCHEDULED" -> PlanStatus.Scheduled
    "ACTIVE" -> PlanStatus.Active
    "COMPLETED" -> PlanStatus.Completed
    "CANCELED" -> PlanStatus.Canceled
    else -> PlanStatus.Unknown
}
