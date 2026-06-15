package com.kiniot.uflex.features.auth.data.mapper

import com.kiniot.uflex.core.session.LocalSession
import com.kiniot.uflex.features.auth.data.remote.dto.SignInResponseDto
import com.kiniot.uflex.features.auth.domain.model.User

fun SignInResponseDto.toDomain(): User {
    return User(
        id = this.id,
        email = this.email,
        roles = this.roles,
        tenantId = this.tenantId
    )
}

fun SignInResponseDto.toLocalSession(): LocalSession {
    return LocalSession(
        userId = this.id,
        patientId = null,
        email = this.email,
        roles = this.roles,
        tenantId = this.tenantId,
        token = this.token
    )
}
