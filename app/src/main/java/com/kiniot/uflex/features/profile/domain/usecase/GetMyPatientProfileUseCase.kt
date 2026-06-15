package com.kiniot.uflex.features.profile.domain.usecase

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.profile.domain.model.PatientProfile
import com.kiniot.uflex.features.profile.domain.repository.ProfileRepository
import javax.inject.Inject

class GetMyPatientProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(): AppResult<PatientProfile> {
        return repository.getMyPatientProfile()
    }
}
