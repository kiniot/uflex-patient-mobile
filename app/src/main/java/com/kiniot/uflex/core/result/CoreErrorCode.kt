package com.kiniot.uflex.core.result

sealed interface CoreErrorCode {
    data object AuthRequired : CoreErrorCode
    data object AccessDenied : CoreErrorCode
    data object BadRequest : CoreErrorCode
    data object Conflict : CoreErrorCode
    data object NotFound : CoreErrorCode
    data object InternalServerError : CoreErrorCode
    data class Http(val status: Int) : CoreErrorCode
    data object Unknown : CoreErrorCode

    companion object {
        fun fromRawCode(code: String?): CoreErrorCode {
            val normalized = code?.trim().orEmpty()

            return when {
                normalized == "AUTH_REQUIRED" -> AuthRequired
                normalized == "ACCESS_DENIED" -> AccessDenied
                normalized == "BAD_REQUEST" -> BadRequest
                normalized == "CONFLICT" -> Conflict
                normalized == "NOT_FOUND" -> NotFound
                normalized == "INTERNAL_SERVER_ERROR" -> InternalServerError
                normalized.startsWith("HTTP_") -> {
                    val status = normalized.removePrefix("HTTP_").toIntOrNull()
                    if (status != null) Http(status) else Unknown
                }

                else -> Unknown
            }
        }

        fun fromStatus(status: Int): CoreErrorCode {
            return when (status) {
                400 -> BadRequest
                401 -> AuthRequired
                403 -> AccessDenied
                404 -> NotFound
                409 -> Conflict
                500 -> InternalServerError
                in 500..599 -> Http(status)
                else -> Http(status)
            }
        }
    }
}
