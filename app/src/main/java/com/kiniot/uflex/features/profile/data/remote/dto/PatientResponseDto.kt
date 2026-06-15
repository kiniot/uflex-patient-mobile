package com.kiniot.uflex.features.profile.data.remote.dto

import com.kiniot.uflex.core.serializers.LocalDateSerializer
import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class PatientResponseDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dni: String,
    @Serializable(with = LocalDateSerializer::class)
    val birthDate: LocalDate,
    val gender: String,
    val email: String,
    val countryCode: String,
    val phoneNumber: String,
    val medicalCondition: String,
    val assignedPhysiotherapistId: String,
    val status: String,
    val clinicId: String
)
