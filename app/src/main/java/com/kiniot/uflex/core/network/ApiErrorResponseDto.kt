package com.kiniot.uflex.core.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponseDto(
    val code: String? = null,
    val message: String? = null,
    val status: Int? = null,
    val title: String? = null,
    val timestamp: String? = null,
    val path: String? = null
)
