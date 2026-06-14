package com.kiniot.uflex.features.auth.data.mapper

import com.kiniot.uflex.features.auth.data.remote.dto.SignInResponseDto
import com.kiniot.uflex.features.auth.domain.model.User

fun SignInResponseDto.toDomain(): User {
    return User(
        id = this.id,
        email = this.email
    )
}
