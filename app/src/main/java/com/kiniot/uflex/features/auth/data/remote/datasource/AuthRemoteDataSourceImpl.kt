package com.kiniot.uflex.features.auth.data.remote.datasource

import com.kiniot.uflex.core.network.SafeApiCaller
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.auth.data.remote.api.AuthApiService
import com.kiniot.uflex.features.auth.data.remote.dto.SignInRequestDto
import com.kiniot.uflex.features.auth.data.remote.dto.SignInResponseDto
import javax.inject.Inject

class AuthRemoteDataSourceImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val safeApiCaller: SafeApiCaller
) : AuthRemoteDataSource {
    override suspend fun signIn(request: SignInRequestDto): AppResult<SignInResponseDto> {
        return safeApiCaller.execute {
            apiService.signIn(request)
        }
    }
}
