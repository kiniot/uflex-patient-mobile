package com.kiniot.uflex.features.auth.data.remote.api

import com.kiniot.uflex.features.auth.data.remote.dto.SignInRequestDto
import com.kiniot.uflex.features.auth.data.remote.dto.SignInResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("authentication/sign-in")
    suspend fun signIn(@Body request: SignInRequestDto): Response<SignInResponseDto>
}
