package com.kiniot.uflex.features.therapy.presentation.preparation

import com.kiniot.uflex.R
import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.device.domain.model.BleConnectionState

/** Human-readable label for the live BLE state shown while connecting. */
fun BleConnectionState.toUiText(): UiText = when (this) {
    BleConnectionState.Idle -> UiText.Resource(R.string.ble_state_connecting)
    BleConnectionState.Scanning -> UiText.Resource(R.string.ble_state_scanning)
    BleConnectionState.Connecting -> UiText.Resource(R.string.ble_state_connecting)
    BleConnectionState.ConfirmingIdentity -> UiText.Resource(R.string.ble_state_confirming)
    BleConnectionState.Connected -> UiText.Resource(R.string.ble_state_connected)
    BleConnectionState.Disconnected -> UiText.Resource(R.string.ble_state_disconnected)
    is BleConnectionState.Failed -> reason.toUiText()
}

fun BleConnectionState.Failed.Reason.toUiText(): UiText = when (this) {
    BleConnectionState.Failed.Reason.DeviceNotFound -> UiText.Resource(R.string.ble_fail_device_not_found)
    BleConnectionState.Failed.Reason.ConnectionLost -> UiText.Resource(R.string.ble_fail_connection_lost)
    BleConnectionState.Failed.Reason.IdentityMismatch -> UiText.Resource(R.string.ble_fail_identity_mismatch)
    BleConnectionState.Failed.Reason.MissingService -> UiText.Resource(R.string.ble_fail_missing_service)
    BleConnectionState.Failed.Reason.BluetoothUnavailable -> UiText.Resource(R.string.ble_fail_bluetooth_unavailable)
    BleConnectionState.Failed.Reason.PermissionDenied -> UiText.Resource(R.string.ble_fail_permission_denied)
    BleConnectionState.Failed.Reason.Unknown -> UiText.Resource(R.string.ble_fail_unknown)
}

/** Step index (0..3) for the progress indicator, or -1 to hide it. */
fun SessionPreparationUiState.Phase.stepIndex(): Int = when (this) {
    SessionPreparationUiState.Phase.Summary,
    SessionPreparationUiState.Phase.Resume,
    SessionPreparationUiState.Phase.Initiating -> 0
    SessionPreparationUiState.Phase.Connecting -> 1
    SessionPreparationUiState.Phase.AwaitingSensors -> 2
    SessionPreparationUiState.Phase.Starting,
    SessionPreparationUiState.Phase.Started -> 3
    else -> -1
}
