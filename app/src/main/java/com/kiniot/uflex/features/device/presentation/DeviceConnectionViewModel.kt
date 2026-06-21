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
            .onEach { state -> _uiState.update { it.copy(connectionState = state) } }
            .launchIn(viewModelScope)

        observeDeviceConnectionStateUseCase()
            .flatMapLatest { state ->
                if (state is BleConnectionState.Connected) {
                    observeMotionTelemetryUseCase()
                } else {
                    emptyFlow()
                }
            }
            .onEach { frame ->
                _uiState.update {
                    it.copy(latestTelemetry = frame, framesReceived = it.framesReceived + 1)
                }
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

    fun onConnect() {
        _uiState.update { it.copy(latestTelemetry = null, framesReceived = 0) }
        viewModelScope.launch { connectToAssignedDeviceUseCase() }
    }

    fun onDisconnect() {
        viewModelScope.launch { disconnectDeviceUseCase() }
    }
}
