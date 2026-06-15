package com.kiniot.uflex.core.result

import com.kiniot.uflex.R
import com.kiniot.uflex.core.ui.UiText

fun AppError.toUserMessage(): UiText {
    return when (this) {
        AppError.Network -> UiText.Resource(R.string.core_error_network)
        AppError.Unauthorized -> UiText.Resource(R.string.core_error_auth_required)
        AppError.Forbidden -> UiText.Resource(R.string.core_error_access_denied)
        AppError.BadRequest -> UiText.Resource(R.string.core_error_bad_request)
        AppError.Conflict -> UiText.Resource(R.string.core_error_conflict)
        AppError.NotFound -> UiText.Resource(R.string.core_error_not_found)
        AppError.Server -> UiText.Resource(R.string.core_error_server)
        is AppError.Business -> status.toFallbackMessage()

        is AppError.Unknown -> UiText.Resource(R.string.core_error_unknown)
    }
}

private fun Int?.toFallbackMessage(): UiText {
    return when (this) {
        400 -> UiText.Resource(R.string.core_error_bad_request)
        401 -> UiText.Resource(R.string.core_error_auth_required)
        403 -> UiText.Resource(R.string.core_error_access_denied)
        404 -> UiText.Resource(R.string.core_error_not_found)
        409 -> UiText.Resource(R.string.core_error_conflict)
        in 500..599 -> UiText.Resource(R.string.core_error_server)
        else -> UiText.Resource(R.string.core_error_unknown)
    }
}
