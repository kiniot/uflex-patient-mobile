package com.kiniot.uflex.features.device.domain.model

/**
 * One decoded BLE motion-telemetry frame from the uFlex kit.
 *
 * It carries the three relative joint rotations (upper-middle, middle-lower, upper-lower) as
 * full quaternions, the device's local actuator feedback state, and a monotonically increasing
 * sequence number used to detect dropped frames and confirm a live stream.
 *
 * [jointFlexionDegrees] is the calibrated, gravity-anchored flexion angle of the active joint the
 * firmware also uses for safety/edge — the source of truth for the on-screen gauge (reads 0 at the
 * calibrated reference and does not drift). It is null / [isCalibrated] false until a session zero
 * is captured, and absent when connected to older firmware that doesn't send the extended frame.
 */
data class MotionTelemetry(
    val upperMiddleRotation: Quaternion,
    val middleLowerRotation: Quaternion,
    val upperLowerRotation: Quaternion,
    val ledColor: LedColor,
    val buzzerActive: Boolean,
    val vibrationActive: Boolean,
    val sequenceNumber: Int,
    val jointFlexionDegrees: Float? = null,
    val isCalibrated: Boolean = false,
    val activeJoint: Int? = null
)
