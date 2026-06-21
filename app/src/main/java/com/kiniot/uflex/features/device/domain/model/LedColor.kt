package com.kiniot.uflex.features.device.domain.model

/**
 * Mirrors the firmware's RgbLed::Color enum, carried in the BLE telemetry frame so the app
 * can reflect the device's local feedback state without recomputing any clinical thresholds.
 * The wire value is the ordinal; [Unknown] is the fallback for forward compatibility.
 */
enum class LedColor {
    Off,
    Red,
    Green,
    Blue,
    Yellow,
    Cyan,
    Magenta,
    Unknown;

    companion object {
        fun fromWire(value: Int): LedColor = when (value) {
            0 -> Off
            1 -> Red
            2 -> Green
            3 -> Blue
            4 -> Yellow
            5 -> Cyan
            6 -> Magenta
            else -> Unknown
        }
    }
}
