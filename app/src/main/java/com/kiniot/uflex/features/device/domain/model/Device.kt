package com.kiniot.uflex.features.device.domain.model

import java.time.LocalDateTime

data class Device(
    val id: String,
    val serialNumber: String,
    val macAddress: String,
    val firmwareVersion: String,
    val batteryLevel: Int,
    val model: String,
    val advertisedName: String,
    val calibrationStatus: DeviceCalibrationStatus,
    val status: DeviceStatus,
    val lastSeenAt: LocalDateTime?,
    val tenantId: String,
    val currentPatientId: String?,
    val offline: Boolean
)
