package com.kiniot.uflex.features.auth.error

enum class AuthErrorCode {
    INVALID_CREDENTIALS,
    USER_WITH_EMAIL_NOT_FOUND,
    USER_WITH_ID_NOT_FOUND,
    UNKNOWN;

    companion object {
        fun fromRawCode(code: String?): AuthErrorCode {
            val normalized = code?.trim().orEmpty()
            return entries.firstOrNull { it.name == normalized } ?: UNKNOWN
        }
    }
}
