package com.kiniot.uflex.core.session

data class LocalSession(
    val userId: String,
    val patientId: String? = null,
    val email: String,
    val roles: List<String>,
    val tenantId: String,
    val token: String
)
