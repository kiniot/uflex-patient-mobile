package com.kiniot.uflex.features.auth.domain.usecase

import com.kiniot.uflex.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class HasActiveSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Boolean {
        return repository.hasActiveSession()
    }
}
