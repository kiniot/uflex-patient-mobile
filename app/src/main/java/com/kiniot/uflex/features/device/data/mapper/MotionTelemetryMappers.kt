package com.kiniot.uflex.features.device.data.mapper

import com.kiniot.uflex.features.device.data.ble.UflexGattProfile
import com.kiniot.uflex.features.device.domain.model.EulerAngles
import com.kiniot.uflex.features.device.domain.model.LedColor
import com.kiniot.uflex.features.device.domain.model.MotionTelemetry
import com.kiniot.uflex.features.device.domain.model.Quaternion
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.asin
import kotlin.math.atan2

/**
 * Decodes the firmware's fixed 53-byte little-endian telemetry frame into a [MotionTelemetry].
 * This is the Kotlin counterpart of scripts/decode_ble_telemetry.py in uflex-embedded-app and
 * must stay in sync with ble_motion_telemetry_serializer.h.
 *
 * Wire layout (all multi-byte fields little-endian):
 *   0..47  : 3 quaternions, each (w, x, y, z) as float32
 *   48     : ledColor        (uint8)
 *   49     : buzzerActive     (uint8, 0/1)
 *   50     : vibrationActive  (uint8, 0/1)
 *   51..52 : sequenceNumber   (uint16)
 *
 * @return the decoded frame, or null when the payload is shorter than the expected size.
 */
fun ByteArray.toMotionTelemetry(): MotionTelemetry? {
    if (size < UflexGattProfile.TELEMETRY_WIRE_SIZE_BYTES) {
        return null
    }

    val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)

    val upperMiddle = buffer.readQuaternion()
    val middleLower = buffer.readQuaternion()
    val upperLower = buffer.readQuaternion()

    val ledColor = LedColor.fromWire(buffer.get().toInt() and 0xFF)
    val buzzerActive = (buffer.get().toInt() and 0xFF) != 0
    val vibrationActive = (buffer.get().toInt() and 0xFF) != 0
    val sequenceNumber = buffer.short.toInt() and 0xFFFF

    return MotionTelemetry(
        upperMiddleRotation = upperMiddle,
        middleLowerRotation = middleLower,
        upperLowerRotation = upperLower,
        ledColor = ledColor,
        buzzerActive = buzzerActive,
        vibrationActive = vibrationActive,
        sequenceNumber = sequenceNumber
    )
}

/**
 * Converts a [Quaternion] (w, x, y, z) into roll/pitch/yaw in degrees, matching the conversion
 * used by the Python decoder so on-device and on-app readings agree.
 */
fun Quaternion.toEulerAngles(): EulerAngles {
    val roll = atan2(2.0 * (w * x + y * z), 1.0 - 2.0 * (x * x + y * y))

    val sinPitch = (2.0 * (w * y - z * x)).coerceIn(-1.0, 1.0)
    val pitch = asin(sinPitch)

    val yaw = atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (y * y + z * z))

    return EulerAngles(
        rollDegrees = Math.toDegrees(roll).toFloat(),
        pitchDegrees = Math.toDegrees(pitch).toFloat(),
        yawDegrees = Math.toDegrees(yaw).toFloat()
    )
}

private fun ByteBuffer.readQuaternion(): Quaternion {
    val w = float
    val x = float
    val y = float
    val z = float
    return Quaternion(w = w, x = x, y = y, z = z)
}
