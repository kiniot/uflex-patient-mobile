package com.kiniot.uflex.features.device.presentation

sealed class DeviceUiState {
    data object Loading : DeviceUiState()
    data object NoDevice : DeviceUiState()
    data class DeviceAssigned(
        val deviceName: String,
        val model: String,
        val serialNumber: String,
    ) : DeviceUiState()
    data object SyncStep1 : DeviceUiState()
    data object SyncStep2Pairing : DeviceUiState()
    data class DeviceConnected(val batteryLevel: Int) : DeviceUiState()
}
