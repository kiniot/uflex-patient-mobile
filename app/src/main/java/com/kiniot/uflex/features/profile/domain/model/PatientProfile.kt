package com.kiniot.uflex.features.profile.domain.model

import java.time.LocalDate

data class PatientProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dni: String,
    val birthDate: LocalDate,
    val gender: PatientGender,
    val email: String,
    val countryCode: String,
    val phoneNumber: String,
    val medicalCondition: String,
    val assignedPhysiotherapistId: String,
    val status: PatientStatus,
    val tenantId: String
)
