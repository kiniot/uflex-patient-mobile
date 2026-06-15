package com.kiniot.uflex.features.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SignInResponseDto(
    val id: String,
    val email: String,
    val roles: List<String>,
    val tenantId: String,
    val token: String
)
