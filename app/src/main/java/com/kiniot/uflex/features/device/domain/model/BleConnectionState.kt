package com.kiniot.uflex.features.device.domain.model

/**
 * Lifecycle of the BLE link to the assigned uFlex kit. The progression mirrors the identity
 * contract: discover by service UUID, connect, then confirm the kit serial before the link is
 * considered usable (see device-identity-contract).
 */
sealed interface BleConnectionState {
    data object Idle : BleConnectionState
    data object Scanning : BleConnectionState
    data object Connecting : BleConnectionState
    data object ConfirmingIdentity : BleConnectionState
    data object Connected : BleConnectionState
    data object Disconnected : BleConnectionState
    data class Failed(val reason: Reason) : BleConnectionState {
        enum class Reason {
            DeviceNotFound,
            ConnectionLost,
            IdentityMismatch,
            MissingService,
            BluetoothUnavailable,
            PermissionDenied,
            Unknown
        }
    }
}
