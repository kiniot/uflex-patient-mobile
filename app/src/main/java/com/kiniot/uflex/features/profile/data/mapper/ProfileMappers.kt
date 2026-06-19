package com.kiniot.uflex.features.profile.data.mapper

import com.kiniot.uflex.features.profile.data.remote.dto.PatientResponseDto
import com.kiniot.uflex.features.profile.domain.model.PatientGender
import com.kiniot.uflex.features.profile.domain.model.PatientProfile
import com.kiniot.uflex.features.profile.domain.model.PatientStatus

fun PatientResponseDto.toDomain(): PatientProfile {
    return PatientProfile(
        id = id,
        firstName = firstName,
        lastName = lastName,
        dni = dni,
        birthDate = birthDate,
        gender = gender.toPatientGender(),
        email = email,
        countryCode = countryCode,
        phoneNumber = phoneNumber,
        medicalCondition = medicalCondition,
        assignedPhysiotherapistId = assignedPhysiotherapistId,
        status = status.toPatientStatus(),
        tenantId = clinicId
    )
}

private fun String.toPatientGender(): PatientGender {
    return when (this) {
        "MALE" -> PatientGender.Male
        "FEMALE" -> PatientGender.Female
        else -> PatientGender.Unknown
    }
}

private fun String.toPatientStatus(): PatientStatus {
    return when (this) {
        "UNASSIGNED" -> PatientStatus.Unassigned
        "IN_TREATMENT" -> PatientStatus.InTreatment
        "COMPLETED" -> PatientStatus.Completed
        "DISCHARGED" -> PatientStatus.Discharged
        "INACTIVE" -> PatientStatus.Inactive
        else -> PatientStatus.Unknown
    }
}
