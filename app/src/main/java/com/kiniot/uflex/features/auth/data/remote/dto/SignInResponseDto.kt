package com.kiniot.uflex.features.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SignInResponseDto(
    val id: String,
    val email: String,
    val token: String
)
