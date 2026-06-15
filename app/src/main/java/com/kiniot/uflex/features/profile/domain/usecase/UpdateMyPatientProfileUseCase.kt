package com.kiniot.uflex.features.profile.domain.usecase

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.profile.domain.model.PatientProfile
import com.kiniot.uflex.features.profile.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdateMyPatientProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(
        email: String,
        countryCode: String,
        phoneNumber: String
    ): AppResult<PatientProfile> {
        return repository.updateMyPatientProfile(
            email = email,
            countryCode = countryCode,
            phoneNumber = phoneNumber
        )
    }
}
