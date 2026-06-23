package com.kiniot.uflex.features.therapy.presentation.execution

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kiniot.uflex.R
import com.kiniot.uflex.core.designsystem.components.feedback.AppSnackbarMessage
import com.kiniot.uflex.core.designsystem.components.feedback.SnackbarManager
import com.kiniot.uflex.core.designsystem.components.feedback.SnackbarType
import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.core.result.toUserMessage
import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.usecase.ConnectToAssignedDeviceUseCase
import com.kiniot.uflex.features.device.domain.usecase.GetMyAssignedDeviceUseCase
import com.kiniot.uflex.features.device.domain.usecase.ObserveDeviceConnectionStateUseCase
import com.kiniot.uflex.features.device.domain.usecase.ObserveMotionTelemetryUseCase
import com.kiniot.uflex.features.therapy.domain.model.LiveRepEvent
import com.kiniot.uflex.features.therapy.domain.model.SessionStatus
import com.kiniot.uflex.features.therapy.domain.usecase.FinalizeSessionUseCase
import com.kiniot.uflex.features.therapy.domain.usecase.GetProgressUseCase
import com.kiniot.uflex.features.therapy.domain.usecase.ObserveLiveProgressUseCase
import com.kiniot.uflex.features.therapy.domain.usecase.ReportPainUseCase
import com.kiniot.uflex.features.therapy.domain.usecase.StartSerieUseCase
import com.kiniot.uflex.features.therapy.navigation.SessionExecutionRoute
import com.kiniot.uflex.features.therapy.presentation.execution.SessionExecutionUiState.Phase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val POLL_INTERVAL_MS = 2_500L

// How long to keep the patient in the reference pose after starting a serie, so the
// kit (which zeros on the new serie via the edge down-channel) captures a clean zero.
private const val CALIBRATION_HOLD_MS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionExecutionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProgressUseCase: GetProgressUseCase,
    private val startSerieUseCase: StartSerieUseCase,
    private val reportPainUseCase: ReportPainUseCase,
    private val finalizeSessionUseCase: FinalizeSessionUseCase,
    private val connectToAssignedDeviceUseCase: ConnectToAssignedDeviceUseCase,
    private val getMyAssignedDeviceUseCase: GetMyAssignedDeviceUseCase,
    private val observeLiveProgressUseCase: ObserveLiveProgressUseCase,
    observeDeviceConnectionStateUseCase: ObserveDeviceConnectionStateUseCase,
    observeMotionTelemetryUseCase: ObserveMotionTelemetryUseCase,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    private val sessionId: String = savedStateHandle.toRoute<SessionExecutionRoute>().sessionId

    private val _uiState = MutableStateFlow(SessionExecutionUiState())
    val uiState: StateFlow<SessionExecutionUiState> = _uiState.asStateFlow()

    private val dismissChannel = Channel<Unit>(Channel.BUFFERED)
    val dismiss = dismissChannel.receiveAsFlow()

    init {
        // Live BLE state for the gauge/connection chip (reuses the link from preparation).
        observeDeviceConnectionStateUseCase()
            .onEach { state -> _uiState.update { it.copy(connectionState = state) } }
            .launchIn(viewModelScope)

        observeDeviceConnectionStateUseCase()
            .flatMapLatest { state ->
                if (state is BleConnectionState.Connected) observeMotionTelemetryUseCase() else emptyFlow()
            }
            .onEach { frame -> _uiState.update { it.copy(latestTelemetry = frame) } }
            .launchIn(viewModelScope)

        startProgressPolling()
        startLiveProgress()
    }

    private fun startProgressPolling() {
        viewModelScope.launch {
            while (isActive && _uiState.value.phase != Phase.Finished) {
                refreshProgress()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun refreshProgress() {
        when (val result = getProgressUseCase(sessionId)) {
            is AppResult.Success -> {
                val progress = result.data
                _uiState.update {
                    it.copy(
                        progress = progress,
                        phase = if (it.phase == Phase.Loading) Phase.Active else it.phase
                    )
                }
                // The session may have been finalized/cancelled elsewhere.
                if (progress.status == SessionStatus.Completed || progress.status == SessionStatus.Cancelled) {
                    finishAndDismiss()
                }
            }

            is AppResult.Error -> {
                // Only a *first* failure (no data yet) is fatal; transient poll errors keep the last state.
                if (_uiState.value.progress == null) {
                    _uiState.update { it.copy(phase = Phase.Failed, errorMessage = result.error.toUserMessage()) }
                }
            }
        }
    }

    /**
     * Optimistic live rep counting via the edge SSE stream. Best-effort: the backend poll
     * above stays authoritative and reconciles every cycle. Failures fall back to polling
     * silently (no user-facing error); transient LAN blips reconnect with a capped backoff.
     */
    private fun startLiveProgress() {
        viewModelScope.launch {
            val serial = (getMyAssignedDeviceUseCase() as? AppResult.Success)?.data?.serialNumber
                ?: return@launch
            observeLiveProgressUseCase(serial)
                .retryWhen { _, attempt ->
                    delay((1_000L * (attempt + 1)).coerceAtMost(10_000L))
                    true  // keep reconnecting; polling covers the gaps meanwhile
                }
                .onEach { event -> applyOptimisticRep(event) }
                .catch { /* give up on SSE; polling carries on */ }
                .launchIn(viewModelScope)
        }
    }

    /** Bump the running serie's rep count optimistically (absolute tally, never backwards). */
    private fun applyOptimisticRep(event: LiveRepEvent) = _uiState.update { state ->
        val progress = state.progress ?: return@update state
        val running = state.runningSerie ?: return@update state
        if (event.serieId != running.serieId) return@update state
        val optimistic = event.repsDetected.coerceAtMost(running.targetRepetitions)
        if (optimistic <= running.currentRepetitions) return@update state
        val series = progress.series.map {
            if (it.serieId == running.serieId) it.copy(currentRepetitions = optimistic) else it
        }
        state.copy(progress = progress.copy(series = series))
    }

    /** Step 1 of the guided start: cue the patient into the reference pose. */
    fun onRequestStartSerie() {
        if (_uiState.value.nextPendingSerie == null) return
        _uiState.update { it.copy(calibrationPromptVisible = true) }
    }

    fun onDismissCalibrationPrompt() =
        _uiState.update { it.copy(calibrationPromptVisible = false) }

    /** Step 2: start the serie, then hold the pose while the kit captures its zero. */
    fun onConfirmStartSerie() {
        val serieId = _uiState.value.nextPendingSerie?.serieId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(calibrationPromptVisible = false, isStartingSerie = true) }
            when (val result = startSerieUseCase(sessionId, serieId)) {
                is AppResult.Success -> {
                    // The kit zeros when it sees the new serie via the edge down-channel;
                    // keep the patient in the reference pose during that window.
                    _uiState.update { it.copy(isStartingSerie = false, isCalibrating = true) }
                    delay(CALIBRATION_HOLD_MS)
                    _uiState.update { it.copy(isCalibrating = false) }
                    refreshProgress()
                }

                is AppResult.Error -> {
                    notifyError(result.error)
                    _uiState.update { it.copy(isStartingSerie = false) }
                }
            }
        }
    }

    fun onShowPainDialog() = _uiState.update { it.copy(painDialogVisible = true) }

    fun onDismissPainDialog() = _uiState.update { it.copy(painDialogVisible = false) }

    fun onReportPain(level: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(painDialogVisible = false) }
            when (val result = reportPainUseCase(sessionId, level)) {
                is AppResult.Success -> notifySuccess(R.string.therapy_exec_pain_reported)
                is AppResult.Error -> notifyError(result.error)
            }
        }
    }

    fun onFinalize() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFinalizing = true) }
            when (val result = finalizeSessionUseCase(sessionId)) {
                is AppResult.Success -> finishAndDismiss()
                is AppResult.Error -> {
                    notifyError(result.error)
                    _uiState.update { it.copy(isFinalizing = false) }
                }
            }
        }
    }

    fun onReconnect() {
        viewModelScope.launch { connectToAssignedDeviceUseCase() }
    }

    private suspend fun finishAndDismiss() {
        _uiState.update { it.copy(phase = Phase.Finished, isFinalizing = false) }
        dismissChannel.send(Unit)
    }

    private fun notifyError(error: AppError) {
        viewModelScope.launch {
            snackbarManager.showMessage(AppSnackbarMessage(error.toUserMessage(), SnackbarType.Error))
        }
    }

    private fun notifySuccess(@StringRes resId: Int) {
        viewModelScope.launch {
            snackbarManager.showMessage(AppSnackbarMessage(UiText.Resource(resId), SnackbarType.Success))
        }
    }
}
