package com.kiniot.uflex.core.result

sealed interface AppError {
    data object Network : AppError
    data object Unauthorized : AppError
    data object Forbidden : AppError
    data object BadRequest : AppError
    data object Conflict : AppError
    data object NotFound : AppError
    data object Server : AppError
    data class Business(
        val code: String,
        val backendMessage: String? = null,
        val status: Int? = null
    ) : AppError
    data class Unknown(val cause: Throwable? = null) : AppError
}
