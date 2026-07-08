package com.kiniot.uflex.features.history.presentation

import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.history.domain.model.HistorySession

data class HistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<HistorySession> = emptyList(),
    val metrics: HistoryMetrics = HistoryMetrics(),
    val errorMessage: UiText? = null
)
