package com.kiniot.uflex.features.plan.domain.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.plan.domain.model.Exercise
import com.kiniot.uflex.features.plan.domain.model.PlansOverview

interface PlanRepository {
    /** The patient's active plan (null when none) plus their scheduled plans, exercises enriched. */
    suspend fun getPlansOverview(): AppResult<PlansOverview>
}

interface ExerciseRepository {
    /** Catalog detail of an exercise, including a fresh signed video URL. */
    suspend fun getExercise(id: String): AppResult<Exercise>
}
