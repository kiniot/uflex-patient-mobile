package com.kiniot.uflex.features.media.data.remote

import com.kiniot.uflex.core.network.StorageHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads raw file bytes directly to Supabase Storage using a signed upload URL.
 *
 * Uses a dedicated OkHttp client WITHOUT [com.kiniot.uflex.core.network.AuthInterceptor]
 * so our backend JWT is never leaked to Supabase. The signed URL already carries
 * its own token.
 */
@Singleton
class SupabaseMediaUploader @Inject constructor(
    @StorageHttpClient private val client: OkHttpClient,
) {
    suspend fun upload(uploadUrl: String, bytes: ByteArray, contentType: String) =
        withContext(Dispatchers.IO) {
            val body = bytes.toRequestBody(contentType.toMediaTypeOrNull())
            val request = Request.Builder()
                .url(uploadUrl)
                .put(body)
                .addHeader("Content-Type", contentType)
                .addHeader("x-upsert", "true")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Supabase upload failed with HTTP ${response.code}")
                }
            }
        }
}
