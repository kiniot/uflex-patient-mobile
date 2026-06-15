package com.kiniot.uflex.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.auth.domain.usecase.SignOutUseCase
import com.kiniot.uflex.features.profile.domain.usecase.GetMyPatientProfileUseCase
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
class ProfileViewModel @Inject constructor(
    private val getMyPatientProfileUseCase: GetMyPatientProfileUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val signOutSuccessChannel = Channel<Unit>(capacity = Channel.BUFFERED)
    val signOutSuccess = signOutSuccessChannel.receiveAsFlow()

    init {
        loadProfile()
    }

    fun onRetry() {
        loadProfile()
    }

    fun onSignOut() {
        if (_uiState.value.isSigningOut) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOut = true) }
            runCatching { signOutUseCase() }
            _uiState.update { it.copy(isSigningOut = false) }
            signOutSuccessChannel.send(Unit)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            when (val result = getMyPatientProfileUseCase()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = result.data,
                            errorMessage = null
                        )
                    }
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = null,
                            errorMessage = result.error.toProfileUserMessage()
                        )
                    }
                }
            }
        }
    }
}
