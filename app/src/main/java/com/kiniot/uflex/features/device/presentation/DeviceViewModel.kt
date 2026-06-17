package com.kiniot.uflex.features.device.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<DeviceUiState>(DeviceUiState.Loading)
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    init {
        loadMockDevice()
    }

    private fun loadMockDevice() {
        viewModelScope.launch {
            delay(400)
            // Toggle between NoDevice and DeviceAssigned to test both states.
            // Change to NoDevice to see the empty state.
            _uiState.value = DeviceUiState.DeviceAssigned(
                deviceName = "uFlex_001",
                model = "uFlex Sensor v2",
                serialNumber = "UF-2024-001",
            )
        }
    }

    fun onStartSync() {
        _uiState.value = DeviceUiState.SyncStep1
    }

    fun onProceedToPairing() {
        _uiState.value = DeviceUiState.SyncStep2Pairing
        viewModelScope.launch {
            delay(3500)
            _uiState.value = DeviceUiState.DeviceConnected(batteryLevel = 85)
        }
    }

    fun onSyncCancelled() {
        _uiState.value = DeviceUiState.DeviceAssigned(
            deviceName = "uFlex_001",
            model = "uFlex Sensor v2",
            serialNumber = "UF-2024-001",
        )
    }
}
