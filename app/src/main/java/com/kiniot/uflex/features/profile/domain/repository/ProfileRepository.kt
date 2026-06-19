package com.kiniot.uflex.features.profile.domain.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.profile.domain.model.PatientProfile

interface ProfileRepository {
    suspend fun getMyPatientProfile(): AppResult<PatientProfile>
    suspend fun updateMyPatientProfile(
        email: String,
        countryCode: String,
        phoneNumber: String
    ): AppResult<PatientProfile>
}
