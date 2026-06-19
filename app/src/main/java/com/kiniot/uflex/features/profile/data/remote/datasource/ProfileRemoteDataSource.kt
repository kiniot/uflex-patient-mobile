package com.kiniot.uflex.features.profile.data.remote.datasource

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.profile.data.remote.dto.PatientResponseDto
import com.kiniot.uflex.features.profile.data.remote.dto.UpdatePatientProfileRequestDto

interface ProfileRemoteDataSource {
    suspend fun getMyProfile(): AppResult<PatientResponseDto>
    suspend fun updateMyProfile(
        request: UpdatePatientProfileRequestDto
    ): AppResult<PatientResponseDto>
}
