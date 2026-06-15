package com.kiniot.uflex.features.profile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePatientProfileRequestDto(
    val email: String,
    val countryCode: String,
    val phoneNumber: String
)
