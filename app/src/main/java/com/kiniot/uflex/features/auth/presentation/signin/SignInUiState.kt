package com.kiniot.uflex.features.auth.presentation.signin

import com.kiniot.uflex.core.ui.UiText

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isPasswordVisible: Boolean = false
)
