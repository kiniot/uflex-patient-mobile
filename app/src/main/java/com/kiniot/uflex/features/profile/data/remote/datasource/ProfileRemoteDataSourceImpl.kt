package com.kiniot.uflex.features.profile.data.remote.datasource

import com.kiniot.uflex.core.network.SafeApiCaller
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.profile.data.remote.api.ProfileApiService
import com.kiniot.uflex.features.profile.data.remote.dto.PatientResponseDto
import com.kiniot.uflex.features.profile.data.remote.dto.UpdatePatientProfileRequestDto
import javax.inject.Inject

class ProfileRemoteDataSourceImpl @Inject constructor(
    private val apiService: ProfileApiService,
    private val safeApiCaller: SafeApiCaller
) : ProfileRemoteDataSource {
    override suspend fun getMyProfile(): AppResult<PatientResponseDto> {
        return safeApiCaller.execute {
            apiService.getMyProfile()
        }
    }

    override suspend fun updateMyProfile(
        request: UpdatePatientProfileRequestDto
    ): AppResult<PatientResponseDto> {
        return safeApiCaller.execute {
            apiService.updateMyProfile(request)
        }
    }
}
