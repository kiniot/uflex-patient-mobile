package com.kiniot.uflex.features.device.data.remote.dto

import com.kiniot.uflex.core.serializers.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class DeviceResponseDto(
    val id: String,
    val serialNumber: String,
    val macAddress: String,
    val firmwareVersion: String,
    val batteryLevel: Int,
    val model: String,
    val advertisedName: String,
    val calibrationStatus: String,
    val status: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastSeenAt: LocalDateTime?,
    val clinicId: String,
    val currentPatientId: String?,
    val offline: Boolean
)
