package com.kiniot.uflex.features.auth.domain.usecase

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.auth.domain.model.User
import com.kiniot.uflex.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): AppResult<User> {
        return repository.signIn(email, password)
    }
}
