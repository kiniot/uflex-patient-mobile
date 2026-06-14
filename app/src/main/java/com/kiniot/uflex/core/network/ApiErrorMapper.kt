package com.kiniot.uflex.core.network

import com.kiniot.uflex.core.result.AppError
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

        return when {
            code == "AUTH_REQUIRED" -> AppError.Unauthorized
            code == "ACCESS_DENIED" -> AppError.Forbidden
            code == "BAD_REQUEST" -> AppError.BadRequest
            code == "NOT_FOUND" -> AppError.NotFound
            resolvedStatus == 401 -> AppError.Unauthorized
            resolvedStatus == 403 -> AppError.Forbidden
            resolvedStatus == 400 -> AppError.BadRequest
            resolvedStatus == 404 -> AppError.NotFound
            resolvedStatus in 500..599 -> AppError.Server
            code.isNotBlank() -> AppError.Business(
                code = code,
                backendMessage = backendMessage,
                status = resolvedStatus
            )

            else -> AppError.Unknown()
        }
    }
}
