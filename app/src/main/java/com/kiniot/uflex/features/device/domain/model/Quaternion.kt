package com.kiniot.uflex.features.device.domain.model

import kotlin.math.sqrt

/**
 * A unit quaternion describing a 3D orientation, as emitted by the uFlex firmware's
 * orientation filter. Component order matches the BLE wire format: w, x, y, z.
 */
data class Quaternion(
    val w: Float,
    val x: Float,
    val y: Float,
    val z: Float
) {
    val magnitude: Float
        get() = sqrt(w * w + x * x + y * y + z * z)
}
