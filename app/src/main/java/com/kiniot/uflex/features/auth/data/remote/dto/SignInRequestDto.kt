package com.kiniot.uflex.features.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SignInRequestDto(
    val email: String,
    val password: String
)
