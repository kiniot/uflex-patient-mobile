package com.kiniot.uflex.features.auth.data.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.features.auth.data.local.datasource.AuthLocalDataSource
import com.kiniot.uflex.features.auth.data.mapper.toLocalSession
import com.kiniot.uflex.features.auth.data.remote.datasource.AuthRemoteDataSource
import com.kiniot.uflex.features.auth.data.mapper.toDomain
import com.kiniot.uflex.features.auth.data.remote.dto.SignInRequestDto
import com.kiniot.uflex.features.auth.domain.model.User
import com.kiniot.uflex.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val localDataSource: AuthLocalDataSource
) : AuthRepository {
    override suspend fun signIn(email: String, password: String): AppResult<User> {
        val request = SignInRequestDto(email = email, password = password)
        return when (val response = remoteDataSource.signIn(request)) {
            is AppResult.Success -> {
                try {
                    localDataSource.saveSession(response.data.toLocalSession())
                    AppResult.Success(response.data.toDomain())
                } catch (exception: Exception) {
                    AppResult.Error(AppError.Unknown(exception))
                }
            }
            is AppResult.Error -> response
        }
    }

    override suspend fun savePatientId(patientId: String) {
        localDataSource.savePatientId(patientId)
    }

    override suspend fun clearSession() {
        localDataSource.clearSession()
    }

    override suspend fun hasActiveSession(): Boolean {
        return localDataSource.isSessionActive()
    }
}
