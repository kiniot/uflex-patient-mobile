package com.kiniot.uflex.features.plan.presentation.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.core.result.toUserMessage
import com.kiniot.uflex.features.plan.domain.usecase.GetMyPlansOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val getMyPlansOverviewUseCase: GetMyPlansOverviewUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExercisesUiState())
    val uiState: StateFlow<ExercisesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getMyPlansOverviewUseCase()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        activePlan = result.data.active,
                        scheduledPlans = result.data.scheduled,
                        errorMessage = null
                    )
                }

                is AppResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.toUserMessage())
                }
            }
        }
    }
}
