package com.kiniot.uflex.features.auth.presentation

import com.kiniot.uflex.core.designsystem.components.feedback.SnackbarType
import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.features.auth.error.AuthErrorCode

fun AppError.shouldShowInlineInSignIn(): Boolean {
    return when (this) {
        is AppError.Business -> when (AuthErrorCode.fromRawCode(code)) {
            AuthErrorCode.INVALID_CREDENTIALS,
            AuthErrorCode.USER_WITH_EMAIL_NOT_FOUND,
            AuthErrorCode.USER_WITH_ID_NOT_FOUND -> true

            AuthErrorCode.UNKNOWN -> false
        }

        else -> false
    }
}

fun AppError.toSnackbarType(): SnackbarType {
    return when (this) {
        is AppError.Network,
        is AppError.Server,
        is AppError.Unknown -> SnackbarType.Error

        else -> SnackbarType.Info
    }
}
