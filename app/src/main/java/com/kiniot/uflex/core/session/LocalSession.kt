package com.kiniot.uflex.core.session

data class LocalSession(
    val userId: String,
    val email: String,
    val roles: List<String>,
    val tenantId: String,
    val token: String
)
