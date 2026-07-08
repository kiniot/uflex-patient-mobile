package com.kiniot.uflex.features.plan.presentation.exercises

import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.plan.domain.model.TreatmentPlan

data class ExercisesUiState(
    val isLoading: Boolean = true,
    val activePlan: TreatmentPlan? = null,
    val scheduledPlans: List<TreatmentPlan> = emptyList(),
    val errorMessage: UiText? = null
)
