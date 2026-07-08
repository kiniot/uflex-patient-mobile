package com.kiniot.uflex.features.therapy.presentation.preparation

import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.device.domain.model.BleConnectionState

data class SessionPreparationUiState(
    val phase: Phase = Phase.Loading,
    val totalSeries: Int = 0,
    val estimatedDurationMinutes: Int = 0,
    val deviceSerial: String? = null,
    val sessionId: String? = null,
    val connectionState: BleConnectionState = BleConnectionState.Idle,
    val errorMessage: UiText? = null
) {
    enum class Phase {
        Loading,
        NoRoutine,
        NoDevice,
        Summary,
        Resume,
        Initiating,
        Connecting,
        AwaitingSensors,
        Starting,
        Started,
        Failed
    }
}
