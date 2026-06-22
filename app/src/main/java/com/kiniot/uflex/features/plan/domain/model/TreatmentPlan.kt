package com.kiniot.uflex.features.plan.domain.model

import java.time.LocalDate

/** A patient's treatment plan with its routines and prescribed exercises. */
data class TreatmentPlan(
    val id: String,
    val name: String,
    val status: PlanStatus,
    val period: PlanPeriod?,
    val routines: List<PlanRoutine>
)

data class PlanPeriod(
    val startsAt: LocalDate?,
    val endsAt: LocalDate?
)

data class PlanRoutine(
    val id: String,
    val name: String,
    val order: Int,
    val schedule: RoutineSchedule?,
    val exercises: List<PlanExercise>
)

data class RoutineSchedule(
    val dayOfWeek: String?,
    val scheduledTime: String?
)

/**
 * One prescribed exercise within a routine: the per-plan dosage (reps, ROM, durations) enriched
 * with the exercise's catalog identity (name, body part, movement) for display.
 */
data class PlanExercise(
    val exerciseId: String,
    val exerciseName: String,
    val bodyPart: BodyPart,
    val movementType: MovementType,
    val order: Int,
    val rangeOfMotionDegrees: Int?,
    val repetitions: Int?,
    val durationSeconds: Int?,
    val restDurationSeconds: Int?
)

/** The patient's plans overview shown in the Exercises tab. */
data class PlansOverview(
    val active: TreatmentPlan?,
    val scheduled: List<TreatmentPlan>
)
