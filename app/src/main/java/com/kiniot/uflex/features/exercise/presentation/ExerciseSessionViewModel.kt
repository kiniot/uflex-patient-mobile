package com.kiniot.uflex.features.exercise.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseSessionUiState(
    val reps: Int = 0,
    val maxReps: Int = 20,
    val postureGood: Boolean = true,
    val isPaused: Boolean = false,
)

@HiltViewModel
class ExerciseSessionViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseSessionUiState())
    val uiState: StateFlow<ExerciseSessionUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null

    init {
        startSession()
    }

    private fun startSession() {
        sessionJob = viewModelScope.launch {
            while (_uiState.value.reps < _uiState.value.maxReps) {
                delay(1200L)
                if (_uiState.value.isPaused) continue
                val newReps = _uiState.value.reps + 1
                // Demo: bad posture at reps 5-7 and 13-15
                val postureGood = newReps !in 5..7 && newReps !in 13..15
                _uiState.update { it.copy(reps = newReps, postureGood = postureGood) }
            }
        }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }
}
