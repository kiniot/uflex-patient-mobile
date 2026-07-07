package com.kiniot.uflex.features.device.presentation

import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.model.Device
import com.kiniot.uflex.features.device.domain.model.MotionTelemetry

/**
 * UI state for the Devices tab: resolve whether the patient has an assigned kit, then drive the
 * guided pairing wizard and the BLE link. [inPairing] is true once the patient enters the wizard
 * (turn-on step onward); it is cleared automatically when the link reaches Connected.
 */
data class DeviceConnectionUiState(
    val deviceCheck: DeviceCheck = DeviceCheck.Loading,
    val connectionState: BleConnectionState = BleConnectionState.Idle,
    val inPairing: Boolean = false,
    val framesReceived: Int = 0,
    val latestTelemetry: MotionTelemetry? = null
) {
    /** A subtle "it's working" signal on the connected view; BLE frames carry no battery/identity. */
    val receivingData: Boolean get() = framesReceived > 0
}

/** Result of asking the backend whether this patient has an assigned device. */
sealed interface DeviceCheck {
    data object Loading : DeviceCheck
    data class Assigned(val device: Device) : DeviceCheck
    data object None : DeviceCheck
    data object Failed : DeviceCheck
}
