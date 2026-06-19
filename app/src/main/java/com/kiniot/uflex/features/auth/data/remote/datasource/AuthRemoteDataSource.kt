package com.kiniot.uflex.features.auth.data.remote.datasource

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.auth.data.remote.dto.SignInRequestDto
import com.kiniot.uflex.features.auth.data.remote.dto.SignInResponseDto

interface AuthRemoteDataSource {
    suspend fun signIn(request: SignInRequestDto): AppResult<SignInResponseDto>
}
