package com.kiniot.uflex.core.network

import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.AppResult
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response

@Singleton
class SafeApiCaller @Inject constructor(
    private val json: Json,
    private val apiErrorMapper: ApiErrorMapper
) {

    suspend fun <T> execute(apiCall: suspend () -> Response<T>): AppResult<T> {
        return try {
            val response = apiCall()

            if (response.isSuccessful) {
                response.body()?.let { AppResult.Success(it) }
                    ?: AppResult.Error(AppError.Server)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = parseErrorResponse(errorBody)
                AppResult.Error(apiErrorMapper.toAppError(response.code(), errorResponse))
            }
        } catch (_: IOException) {
            AppResult.Error(AppError.Network)
        } catch (_: SerializationException) {
            AppResult.Error(AppError.Unknown())
        } catch (exception: Exception) {
            AppResult.Error(AppError.Unknown(exception))
        }
    }

    private fun parseErrorResponse(errorBody: String?): ApiErrorResponseDto? {
        if (errorBody.isNullOrBlank()) {
            return null
        }

        return try {
            json.decodeFromString<ApiErrorResponseDto>(errorBody)
        } catch (_: SerializationException) {
            null
        }
    }
}
