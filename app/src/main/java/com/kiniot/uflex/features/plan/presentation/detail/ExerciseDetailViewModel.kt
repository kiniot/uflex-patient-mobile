package com.kiniot.uflex.features.plan.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.core.result.toUserMessage
import com.kiniot.uflex.features.plan.domain.usecase.GetExerciseDetailUseCase
import com.kiniot.uflex.features.plan.navigation.ExerciseDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExerciseDetailUseCase: GetExerciseDetailUseCase
) : ViewModel() {

    private val exerciseId: String = savedStateHandle.toRoute<ExerciseDetailRoute>().exerciseId

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getExerciseDetailUseCase(exerciseId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoading = false, exercise = result.data, errorMessage = null)
                }

                is AppResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.toUserMessage())
                }
            }
        }
    }
}
