package com.kiniot.uflex.features.device.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.usecase.ConnectToAssignedDeviceUseCase
import com.kiniot.uflex.features.device.domain.usecase.DisconnectDeviceUseCase
import com.kiniot.uflex.features.device.domain.usecase.GetMyAssignedDeviceUseCase
import com.kiniot.uflex.features.device.domain.usecase.ObserveDeviceConnectionStateUseCase
import com.kiniot.uflex.features.device.domain.usecase.ObserveMotionTelemetryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DeviceConnectionViewModel @Inject constructor(
    private val getMyAssignedDeviceUseCase: GetMyAssignedDeviceUseCase,
    private val connectToAssignedDeviceUseCase: ConnectToAssignedDeviceUseCase,
    private val observeDeviceConnectionStateUseCase: ObserveDeviceConnectionStateUseCase,
    private val observeMotionTelemetryUseCase: ObserveMotionTelemetryUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceConnectionUiState())
    val uiState: StateFlow<DeviceConnectionUiState> = _uiState.asStateFlow()

    init {
        observeDeviceConnectionStateUseCase()
            .onEach { state ->
                _uiState.update {
                    it.copy(
                        connectionState = state,
                        // Once the link is up, the wizard is done — the Connected view takes over.
                        inPairing = if (state is BleConnectionState.Connected) false else it.inPairing
                    )
                }
            }
            .launchIn(viewModelScope)

        // Keep the latest frame for liveness and the kit's own LED/status signal.
        observeDeviceConnectionStateUseCase()
            .flatMapLatest { state ->
                if (state is BleConnectionState.Connected) observeMotionTelemetryUseCase() else emptyFlow()
            }
            .onEach { frame ->
                _uiState.update { it.copy(framesReceived = it.framesReceived + 1, latestTelemetry = frame) }
            }
            .launchIn(viewModelScope)

        loadAssignedDevice()
    }

    fun loadAssignedDevice() {
        viewModelScope.launch {
            _uiState.update { it.copy(deviceCheck = DeviceCheck.Loading) }
            val check = when (val result = getMyAssignedDeviceUseCase()) {
                is AppResult.Success -> DeviceCheck.Assigned(result.data)
                is AppResult.Error ->
                    if (result.error is AppError.NotFound) DeviceCheck.None else DeviceCheck.Failed
            }
            _uiState.update { it.copy(deviceCheck = check) }
        }
    }

    /** Enter the pairing wizard (Step 1 "turn on"); does not connect yet. */
    fun startPairing() {
        _uiState.update { it.copy(inPairing = true) }
    }

    /** Leave the wizard, aborting any in-flight connection attempt. */
    fun cancelPairing() {
        val busy = _uiState.value.connectionState.let {
            it is BleConnectionState.Scanning || it is BleConnectionState.Connecting ||
                it is BleConnectionState.ConfirmingIdentity || it is BleConnectionState.Connected
        }
        _uiState.update { it.copy(inPairing = false) }
        if (busy) viewModelScope.launch { disconnectDeviceUseCase() }
    }

    /** Kick off the atomic scan → connect → confirm-identity flow (after permissions + BT are ready). */
    fun onConnect() {
        _uiState.update { it.copy(framesReceived = 0, latestTelemetry = null) }
        viewModelScope.launch { connectToAssignedDeviceUseCase() }
    }

    fun onDisconnect() {
        _uiState.update { it.copy(inPairing = false) }
        viewModelScope.launch { disconnectDeviceUseCase() }
    }
}
