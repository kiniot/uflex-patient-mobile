package com.kiniot.uflex.features.auth.domain.model

data class User(
    val id: String,
    val email: String,
    val roles: List<String>,
    val tenantId: String
)
