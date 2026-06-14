package com.kiniot.uflex.features.auth.data.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.auth.data.remote.datasource.AuthRemoteDataSource
import com.kiniot.uflex.features.auth.data.mapper.toDomain
import com.kiniot.uflex.features.auth.data.remote.dto.SignInRequestDto
import com.kiniot.uflex.features.auth.domain.model.User
import com.kiniot.uflex.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource
) : AuthRepository {
    override suspend fun signIn(email: String, password: String): AppResult<User> {
        val request = SignInRequestDto(email = email, password = password)
        return when (val response = remoteDataSource.signIn(request)) {
            is AppResult.Success -> AppResult.Success(response.data.toDomain())
            is AppResult.Error -> response
        }
    }
}
