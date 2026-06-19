package com.kiniot.uflex.features.profile.presentation

import com.kiniot.uflex.core.ui.UiText

data class EditContactInfoUiState(
    val email: String = "",
    val countryCode: String = "",
    val phoneNumber: String = "",
    val isSaving: Boolean = false,
    val errorMessage: UiText? = null,
    val isInitialized: Boolean = false
)
