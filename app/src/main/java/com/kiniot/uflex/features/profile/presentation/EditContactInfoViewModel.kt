package com.kiniot.uflex.features.profile.presentation

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiniot.uflex.R
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.profile.domain.usecase.UpdateMyPatientProfileUseCase
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
class EditContactInfoViewModel @Inject constructor(
    private val updateMyPatientProfileUseCase: UpdateMyPatientProfileUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditContactInfoUiState())
    val uiState: StateFlow<EditContactInfoUiState> = _uiState.asStateFlow()

    private val saveSuccessChannel = Channel<Unit>(capacity = Channel.BUFFERED)
    val saveSuccess = saveSuccessChannel.receiveAsFlow()

    fun initialize(
        email: String,
        countryCode: String,
        phoneNumber: String
    ) {
        if (_uiState.value.isInitialized) return

        _uiState.update {
            it.copy(
                email = email,
                countryCode = countryCode,
                phoneNumber = phoneNumber,
                isInitialized = true
            )
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onCountryCodeChanged(countryCode: String) {
        _uiState.update { it.copy(countryCode = countryCode, errorMessage = null) }
    }

    fun onPhoneNumberChanged(phoneNumber: String) {
        _uiState.update { it.copy(phoneNumber = phoneNumber, errorMessage = null) }
    }

    fun onSave() {
        val state = _uiState.value
        val validationError = validate(
            email = state.email,
            countryCode = state.countryCode,
            phoneNumber = state.phoneNumber
        )

        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null
                )
            }

            when (
                val result = updateMyPatientProfileUseCase(
                    email = state.email.trim(),
                    countryCode = state.countryCode.trim(),
                    phoneNumber = state.phoneNumber.trim()
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    saveSuccessChannel.send(Unit)
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.error.toProfileUserMessage()
                        )
                    }
                }
            }
        }
    }

    private fun validate(
        email: String,
        countryCode: String,
        phoneNumber: String
    ): UiText? {
        return when {
            email.isBlank() -> UiText.Resource(R.string.profile_edit_contact_error_email_required)
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                UiText.Resource(R.string.profile_edit_contact_error_email_invalid)

            countryCode.isBlank() ->
                UiText.Resource(R.string.profile_edit_contact_error_country_code_required)

            phoneNumber.isBlank() ->
                UiText.Resource(R.string.profile_edit_contact_error_phone_required)

            else -> null
        }
    }
}
