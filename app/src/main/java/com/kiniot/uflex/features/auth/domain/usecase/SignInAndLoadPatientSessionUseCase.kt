package com.kiniot.uflex.features.auth.domain.usecase

import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.auth.domain.repository.AuthRepository
import com.kiniot.uflex.features.profile.domain.usecase.GetMyPatientProfileUseCase
import javax.inject.Inject

class SignInAndLoadPatientSessionUseCase @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val getMyPatientProfileUseCase: GetMyPatientProfileUseCase,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): AppResult<Unit> {
        return when (val signInResult = signInUseCase(email, password)) {
            is AppResult.Success -> {
                when (val patientProfileResult = getMyPatientProfileUseCase()) {
                    is AppResult.Success -> {
                        try {
                            authRepository.savePatientId(patientProfileResult.data.id)
                            AppResult.Success(Unit)
                        } catch (exception: Exception) {
                            authRepository.clearSession()
                            AppResult.Error(AppError.Unknown(exception))
                        }
                    }

                    is AppResult.Error -> {
                        authRepository.clearSession()
                        patientProfileResult
                    }
                }
            }

            is AppResult.Error -> signInResult
        }
    }
}
