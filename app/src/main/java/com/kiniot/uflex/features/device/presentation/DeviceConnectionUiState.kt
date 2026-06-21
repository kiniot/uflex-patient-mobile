package com.kiniot.uflex.features.device.presentation

import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.model.Device
import com.kiniot.uflex.features.device.domain.model.MotionTelemetry

/**
 * UI state for the device-connection proof of concept: first resolve whether the patient has an
 * assigned kit, then drive the BLE link and surface the live telemetry frames.
 */
data class DeviceConnectionUiState(
    val deviceCheck: DeviceCheck = DeviceCheck.Loading,
    val connectionState: BleConnectionState = BleConnectionState.Idle,
    val latestTelemetry: MotionTelemetry? = null,
    val framesReceived: Int = 0
)

/** Result of asking the backend whether this patient has an assigned device. */
sealed interface DeviceCheck {
    data object Loading : DeviceCheck
    data class Assigned(val device: Device) : DeviceCheck
    data object None : DeviceCheck
    data object Failed : DeviceCheck
}
