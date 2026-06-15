package com.kiniot.uflex.features.profile.presentation

import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.profile.domain.model.PatientProfile

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSigningOut: Boolean = false,
    val profile: PatientProfile? = null,
    val errorMessage: UiText? = null
)
