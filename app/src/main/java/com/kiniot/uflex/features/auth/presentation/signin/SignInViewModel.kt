package com.kiniot.uflex.features.auth.presentation.signin

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiniot.uflex.R
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.core.result.toUserMessage
import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.auth.domain.usecase.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val signInSuccessChannel = Channel<Unit>(capacity = Channel.BUFFERED)
    val signInSuccess = signInSuccessChannel.receiveAsFlow()

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                errorMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                errorMessage = null
            )
        }
    }

    fun onPasswordVisibilityToggled() {
        _uiState.update {
            it.copy(isPasswordVisible = !it.isPasswordVisible)
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    fun onSubmit() {
        val currentState = _uiState.value
        val validationError = validate(currentState.email, currentState.password)

        if (validationError != null) {
            _uiState.update {
                it.copy(errorMessage = validationError)
            }
            return
        }

        if (currentState.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            when (val result = signInUseCase(currentState.email.trim(), currentState.password)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    signInSuccessChannel.send(Unit)
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.toUserMessage()
                        )
                    }
                }
            }
        }
    }

    private fun validate(email: String, password: String): UiText? {
        return when {
            email.isBlank() -> UiText.Resource(R.string.auth_sign_in_error_email_required)
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                UiText.Resource(R.string.auth_sign_in_error_email_invalid)

            password.isBlank() -> UiText.Resource(R.string.auth_sign_in_error_password_required)
            else -> null
        }
    }
}
