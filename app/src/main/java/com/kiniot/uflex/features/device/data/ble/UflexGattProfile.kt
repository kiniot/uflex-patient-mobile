package com.kiniot.uflex.features.device.data.ble

import java.util.UUID

/**
 * The uFlex custom GATT profile, kept in sync with the firmware's BleTelemetryServer
 * (see ble_telemetry_server.h in uflex-embedded-app and docs/device-identity-contract.md).
 */
object UflexGattProfile {
    /** Advertised service used to filter the scan to uFlex kits only. */
    val SERVICE_UUID: UUID = UUID.fromString("a1f7b2c0-3e4d-4a5b-8c6d-1f2e3a4b5c6d")

    /** NOTIFY characteristic carrying the 53-byte motion telemetry frame. */
    val TELEMETRY_CHARACTERISTIC_UUID: UUID = UUID.fromString("a1f7b2c1-3e4d-4a5b-8c6d-1f2e3a4b5c6d")

    /** READ characteristic exposing the kit serial, used to confirm device identity. */
    val SERIAL_CHARACTERISTIC_UUID: UUID = UUID.fromString("a1f7b2c2-3e4d-4a5b-8c6d-1f2e3a4b5c6d")

    /** Minimum telemetry wire frame, in bytes: 3 quaternions + actuators + sequence. */
    const val TELEMETRY_WIRE_SIZE_BYTES: Int = 53

    /**
     * Extended frame size, in bytes: the base frame plus the calibrated joint flexion angle
     * (float32) + isCalibrated (uint8) + activeJoint (uint8). Firmware >= the mux/flexion update
     * sends this; older firmware sends only the base 53 bytes (extended fields decode as absent).
     */
    const val TELEMETRY_WIRE_SIZE_EXTENDED_BYTES: Int = 59
}
