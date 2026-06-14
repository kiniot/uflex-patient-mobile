package com.kiniot.uflex.features.auth.domain.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.auth.domain.model.User

interface AuthRepository {
    suspend fun signIn(email: String, password: String): AppResult<User>
}
