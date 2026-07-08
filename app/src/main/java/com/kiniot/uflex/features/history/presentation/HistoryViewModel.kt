package com.kiniot.uflex.features.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiniot.uflex.R
import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.history.domain.usecase.ObserveHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeHistoryUseCase: ObserveHistoryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeHistoryUseCase()
            .onEach { sessions ->
                val ordered = orderedHistorySessions(sessions)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessions = ordered,
                        metrics = calculateHistoryMetrics(ordered),
                        errorMessage = null
                    )
                }
            }
            .catch {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UiText.Resource(R.string.history_load_error)
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
