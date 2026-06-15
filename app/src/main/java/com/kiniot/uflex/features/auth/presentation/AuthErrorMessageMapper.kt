package com.kiniot.uflex.features.auth.presentation

import com.kiniot.uflex.R
import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.toUserMessage
import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.auth.error.AuthErrorCode

fun AppError.toAuthUserMessage(): UiText {
    return when (this) {
        is AppError.Business -> when (AuthErrorCode.fromRawCode(code)) {
            AuthErrorCode.INVALID_CREDENTIALS ->
                UiText.Resource(R.string.auth_sign_in_error_invalid_credentials)

            AuthErrorCode.USER_WITH_EMAIL_NOT_FOUND ->
                UiText.Resource(R.string.auth_sign_in_error_user_with_email_not_found)

            AuthErrorCode.USER_WITH_ID_NOT_FOUND ->
                UiText.Resource(R.string.auth_error_user_with_id_not_found)

            AuthErrorCode.UNKNOWN -> toUserMessage()
        }

        else -> toUserMessage()
    }
}
