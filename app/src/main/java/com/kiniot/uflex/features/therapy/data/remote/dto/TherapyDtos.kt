package com.kiniot.uflex.features.therapy.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DailyScheduleResponseDto(
    val patientId: String? = null,
    val date: String? = null,
    val resolutionStatus: String? = null,
    val routineId: String? = null,
    val totalSeries: Int? = null,
    val estimatedDurationMinutes: Int? = null
)

@Serializable
data class InitiateSessionRequestDto(
    val patientId: String,
    val treatmentPlanId: String,
    val iotDeviceId: String,
    val routineId: String
)

@Serializable
data class ConfirmHardwareRequestDto(
    val sensorsPlaced: Boolean
)

@Serializable
data class CancelSessionRequestDto(
    val reason: String
)

@Serializable
data class TherapySessionResponseDto(
    val id: String,
    val patientId: String? = null,
    val treatmentPlanId: String? = null,
    val iotDeviceId: String? = null,
    val sensorsPlaced: Boolean? = null,
    val status: String? = null,
    val painLevel: Int? = null,
    val requiresClinicalReview: Boolean? = null,
    val startedAt: String? = null,
    val finalizedAt: String? = null
)
