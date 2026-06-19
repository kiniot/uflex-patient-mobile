package com.kiniot.uflex.features.profile.data.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.features.profile.data.mapper.toDomain
import com.kiniot.uflex.features.profile.data.local.datasource.ProfileLocalDataSource
import com.kiniot.uflex.features.profile.data.remote.datasource.ProfileRemoteDataSource
import com.kiniot.uflex.features.profile.data.remote.dto.UpdatePatientProfileRequestDto
import com.kiniot.uflex.features.profile.domain.model.PatientProfile
import com.kiniot.uflex.features.profile.domain.repository.ProfileRepository
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val remoteDataSource: ProfileRemoteDataSource,
    private val localDataSource: ProfileLocalDataSource
) : ProfileRepository {
    override suspend fun getMyPatientProfile(): AppResult<PatientProfile> {
        return when (val response = remoteDataSource.getMyProfile()) {
            is AppResult.Success -> AppResult.Success(response.data.toDomain())
            is AppResult.Error -> response
        }
    }

    override suspend fun updateMyPatientProfile(
        email: String,
        countryCode: String,
        phoneNumber: String
    ): AppResult<PatientProfile> {
        val request = UpdatePatientProfileRequestDto(
            email = email,
            countryCode = countryCode,
            phoneNumber = phoneNumber
        )

        return when (val response = remoteDataSource.updateMyProfile(request)) {
            is AppResult.Success -> {
                try {
                    localDataSource.saveEmail(response.data.email)
                    AppResult.Success(response.data.toDomain())
                } catch (exception: Exception) {
                    AppResult.Error(AppError.Unknown(exception))
                }
            }
            is AppResult.Error -> response
        }
    }
}
