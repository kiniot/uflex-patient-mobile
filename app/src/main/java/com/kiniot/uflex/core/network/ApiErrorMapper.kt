package com.kiniot.uflex.core.network

import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.CoreErrorCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiErrorMapper @Inject constructor() {

    fun toAppError(
        statusCode: Int,
        errorResponse: ApiErrorResponseDto?
    ): AppError {
        val code = errorResponse?.code?.trim().orEmpty()
        val backendMessage = errorResponse?.message
        val resolvedStatus = errorResponse?.status ?: statusCode
        val coreErrorCode = CoreErrorCode.fromRawCode(code)

        return when {
            coreErrorCode != CoreErrorCode.Unknown -> coreErrorCode.toAppError()
            code.isNotBlank() -> AppError.Business(
                code = code,
                backendMessage = backendMessage,
                status = resolvedStatus
            )

            else -> CoreErrorCode.fromStatus(resolvedStatus).toAppError()
        }
    }

    private fun CoreErrorCode.toAppError(): AppError {
        return when (this) {
            CoreErrorCode.AuthRequired -> AppError.Unauthorized
            CoreErrorCode.AccessDenied -> AppError.Forbidden
            CoreErrorCode.BadRequest -> AppError.BadRequest
            CoreErrorCode.Conflict -> AppError.Conflict
            CoreErrorCode.NotFound -> AppError.NotFound
            CoreErrorCode.InternalServerError -> AppError.Server
            is CoreErrorCode.Http -> if (status in 500..599) AppError.Server else AppError.Unknown()
            CoreErrorCode.Unknown -> AppError.Unknown()
        }
    }
}
