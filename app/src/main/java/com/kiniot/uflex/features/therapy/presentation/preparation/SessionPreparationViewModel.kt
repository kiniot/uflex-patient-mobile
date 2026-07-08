package com.kiniot.uflex.features.therapy.presentation.preparation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.core.result.toUserMessage
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.usecase.ConnectToAssignedDeviceUseCase
import com.kiniot.uflex.features.device.domain.usecase.DisconnectDeviceUseCase
import com.kiniot.uflex.features.device.domain.usecase.GetMyAssignedDeviceUseCase
import com.kiniot.uflex.features.device.domain.usecase.ObserveDeviceConnectionStateUseCase
import com.kiniot.uflex.features.therapy.domain.model.ScheduleResolution
import com.kiniot.uflex.features.therapy.domain.model.SessionStatus
import com.kiniot.uflex.features.therapy.domain.usecase.CancelSessionUseCase
import com.kiniot.uflex.features.therapy.domain.usecase.ConfirmHardwareUseCase
import com.kiniot.uflex.features.therapy.domain.usecase.GetActiveSessionUseCase
import com.kiniot.uflex.features.therapy.domain.usecase.GetDailyScheduleUseCase
import com.kiniot.uflex.features.therapy.domain.usecase.InitiateSessionUseCase
import com.kiniot.uflex.features.therapy.domain.usecase.StartSessionUseCase
import com.kiniot.uflex.features.therapy.navigation.SessionPreparationRoute
import com.kiniot.uflex.features.therapy.presentation.preparation.SessionPreparationUiState.Phase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val CANCEL_REASON = "Cancelled by patient"

@HiltViewModel
class SessionPreparationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDailyScheduleUseCase: GetDailyScheduleUseCase,
    private val getActiveSessionUseCase: GetActiveSessionUseCase,
    private val initiateSessionUseCase: InitiateSessionUseCase,
    private val confirmHardwareUseCase: ConfirmHardwareUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val cancelSessionUseCase: CancelSessionUseCase,
    private val getMyAssignedDeviceUseCase: GetMyAssignedDeviceUseCase,
    private val connectToAssignedDeviceUseCase: ConnectToAssignedDeviceUseCase,
    observeDeviceConnectionStateUseCase: ObserveDeviceConnectionStateUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase
) : ViewModel() {

    private val treatmentPlanId: String =
        savedStateHandle.toRoute<SessionPreparationRoute>().treatmentPlanId

    private val _uiState = MutableStateFlow(SessionPreparationUiState())
    val uiState: StateFlow<SessionPreparationUiState> = _uiState.asStateFlow()

    private val dismissChannel = Channel<Unit>(Channel.BUFFERED)
    val dismiss = dismissChannel.receiveAsFlow()

    private var routineId: String? = null
    private var needsConfirm: Boolean = true
    private var needsStart: Boolean = true

    init {
        observeDeviceConnectionStateUseCase()
            .onEach { state ->
                _uiState.update { it.copy(connectionState = state) }
                handleConnectionState(state)
            }
            .launchIn(viewModelScope)
        load()
    }

    fun onRetry() = load()

    /** Fresh start from the summary: create the session, then connect. */
    fun onBegin() {
        val routine = routineId ?: return
        val serial = _uiState.value.deviceSerial ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(phase = Phase.Initiating, errorMessage = null) }
            when (val result = initiateSessionUseCase(treatmentPlanId, routine, serial)) {
                is AppResult.Success -> {
                    needsConfirm = true
                    needsStart = true
                    _uiState.update { it.copy(sessionId = result.data.id) }
                    connect()
                }

                is AppResult.Error ->
                    if (result.error is AppError.Conflict) load() // already active → resume
                    else fail(result.error)
            }
        }
    }

    /** Resume an existing pending/ready session: just connect (confirm/start follow on Connected). */
    fun onResumeConnect() = connect()

    /** Re-establish the BLE link for an already in-progress session (no confirm/start). */
    fun onReconnect() {
        needsConfirm = false
        needsStart = false
        connect()
    }

    fun onConfirmSensors() {
        val id = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(phase = Phase.Starting, errorMessage = null) }
            val confirm = confirmHardwareUseCase(id, sensorsPlaced = true)
            if (confirm is AppResult.Error) {
                fail(confirm.error)
                return@launch
            }
            finishStart(id)
        }
    }

    fun onCancel() {
        viewModelScope.launch {
            _uiState.value.sessionId?.let { cancelSessionUseCase(it, CANCEL_REASON) }
            disconnectDeviceUseCase()
            dismissChannel.send(Unit)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(phase = Phase.Loading, errorMessage = null) }
            when (val active = getActiveSessionUseCase()) {
                is AppResult.Success -> when (active.data.status) {
                    SessionStatus.InProgress -> {
                        // Already running on the backend, but the BLE link is not established here;
                        // the Started screen reflects the live connection and offers a reconnect.
                        needsConfirm = false
                        needsStart = false
                        _uiState.update { it.copy(phase = Phase.Started, sessionId = active.data.id) }
                    }

                    SessionStatus.Ready -> {
                        needsConfirm = false
                        needsStart = true
                        _uiState.update { it.copy(phase = Phase.Resume, sessionId = active.data.id) }
                    }

                    SessionStatus.Pending -> {
                        needsConfirm = true
                        needsStart = true
                        _uiState.update { it.copy(phase = Phase.Resume, sessionId = active.data.id) }
                    }

                    else -> loadSummary()
                }

                is AppResult.Error ->
                    if (active.error is AppError.NotFound) loadSummary() else fail(active.error)
            }
        }
    }

    private suspend fun loadSummary() {
        when (val schedule = getDailyScheduleUseCase()) {
            is AppResult.Success -> {
                val data = schedule.data
                if (data.resolution != ScheduleResolution.Found || data.routineId == null) {
                    _uiState.update { it.copy(phase = Phase.NoRoutine) }
                    return
                }
                routineId = data.routineId
                when (val device = getMyAssignedDeviceUseCase()) {
                    is AppResult.Success -> _uiState.update {
                        it.copy(
                            phase = Phase.Summary,
                            totalSeries = data.totalSeries,
                            estimatedDurationMinutes = data.estimatedDurationMinutes,
                            deviceSerial = device.data.serialNumber
                        )
                    }

                    is AppResult.Error ->
                        if (device.error is AppError.NotFound) _uiState.update { it.copy(phase = Phase.NoDevice) }
                        else fail(device.error)
                }
            }

            is AppResult.Error -> fail(schedule.error)
        }
    }

    private fun connect() {
        _uiState.update { it.copy(phase = Phase.Connecting, errorMessage = null) }
        viewModelScope.launch {
            val result = connectToAssignedDeviceUseCase()
            // The connection-state flow drives the next step on Connected; only the error path
            // (when the flow has not already moved us to Failed) needs handling here.
            if (result is AppResult.Error && _uiState.value.phase == Phase.Connecting &&
                _uiState.value.connectionState !is BleConnectionState.Failed
            ) {
                fail(result.error)
            }
        }
    }

    private fun handleConnectionState(state: BleConnectionState) {
        if (_uiState.value.phase != Phase.Connecting) return
        when (state) {
            is BleConnectionState.Connected ->
                if (needsConfirm) {
                    _uiState.update { it.copy(phase = Phase.AwaitingSensors) }
                } else {
                    val id = _uiState.value.sessionId ?: return
                    viewModelScope.launch { finishStart(id) }
                }

            is BleConnectionState.Failed -> _uiState.update { it.copy(phase = Phase.Failed) }
            else -> Unit
        }
    }

    /** Issues the start call when needed, then lands on Started (connection now established). */
    private suspend fun finishStart(id: String) {
        if (needsStart) {
            _uiState.update { it.copy(phase = Phase.Starting, errorMessage = null) }
            val start = startSessionUseCase(id)
            if (start is AppResult.Error) {
                fail(start.error)
                return
            }
        }
        _uiState.update { it.copy(phase = Phase.Started) }
    }

    private fun fail(error: AppError) {
        _uiState.update { it.copy(phase = Phase.Failed, errorMessage = error.toUserMessage()) }
    }
}
