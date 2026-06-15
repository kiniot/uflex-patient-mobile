package com.kiniot.uflex.features.profile.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import com.kiniot.uflex.features.profile.data.remote.dto.PatientResponseDto
import com.kiniot.uflex.features.profile.data.remote.dto.UpdatePatientProfileRequestDto

interface ProfileApiService {
    @GET("patients/me")
    suspend fun getMyProfile(): Response<PatientResponseDto>

    @PUT("patients/me")
    suspend fun updateMyProfile(
        @Body request: UpdatePatientProfileRequestDto
    ): Response<PatientResponseDto>
}
