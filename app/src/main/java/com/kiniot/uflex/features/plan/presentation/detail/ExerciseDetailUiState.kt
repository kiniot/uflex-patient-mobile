package com.kiniot.uflex.features.plan.presentation.detail

import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.plan.domain.model.Exercise

data class ExerciseDetailUiState(
    val isLoading: Boolean = true,
    val exercise: Exercise? = null,
    val errorMessage: UiText? = null
)
