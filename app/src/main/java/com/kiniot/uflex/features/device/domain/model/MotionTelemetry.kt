package com.kiniot.uflex.features.device.domain.model

/**
 * One decoded BLE motion-telemetry frame from the uFlex kit.
 *
 * It carries the three relative joint rotations (upper-middle, middle-lower, upper-lower) as
 * full quaternions, the device's local actuator feedback state, and a monotonically increasing
 * sequence number used to detect dropped frames and confirm a live stream.
 */
data class MotionTelemetry(
    val upperMiddleRotation: Quaternion,
    val middleLowerRotation: Quaternion,
    val upperLowerRotation: Quaternion,
    val ledColor: LedColor,
    val buzzerActive: Boolean,
    val vibrationActive: Boolean,
    val sequenceNumber: Int
)
