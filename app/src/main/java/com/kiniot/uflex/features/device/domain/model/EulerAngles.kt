package com.kiniot.uflex.features.device.domain.model

/**
 * A quaternion expressed as Euler angles in degrees, which is the clinically meaningful
 * view for range-of-motion. Derived from a [Quaternion] by the telemetry mapper.
 */
data class EulerAngles(
    val rollDegrees: Float,
    val pitchDegrees: Float,
    val yawDegrees: Float
)
