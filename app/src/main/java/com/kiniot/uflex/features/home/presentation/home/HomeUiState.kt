package com.kiniot.uflex.features.home.presentation.home

import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.plan.domain.model.PlanExercise
import com.kiniot.uflex.features.plan.domain.model.TreatmentPlan
import com.kiniot.uflex.features.therapy.domain.model.ScheduleResolution

/** UI state of the Home dashboard. Mirrors the plan feature's UiState shape. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val firstName: String? = null,
    val greeting: Greeting = Greeting.Morning,
    val activePlan: TreatmentPlan? = null,
    val planProgress: PlanProgress? = null,
    val todayResolution: ScheduleResolution = ScheduleResolution.Unknown,
    val todaysExercises: List<PlanExercise> = emptyList(),
    val todayTotalSeries: Int? = null,
    val todayEstMinutes: Int? = null,
    val errorMessage: UiText? = null
)

/**
 * Honest "progress" derived from the active plan's period (how far into the prescribed window we
 * are) — NOT session/adherence completion, for which there is no endpoint.
 */
data class PlanProgress(
    val fraction: Float,
    val percent: Int,
    val dayIndex: Int,
    val totalDays: Int
)

enum class Greeting { Morning, Afternoon, Evening }
