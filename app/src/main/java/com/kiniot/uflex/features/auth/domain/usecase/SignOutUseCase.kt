package com.kiniot.uflex.features.auth.domain.usecase

import com.kiniot.uflex.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() {
        repository.clearSession()
    }
}
