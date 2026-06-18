package com.kiniot.uflex.features.media.data.remote.api

import com.kiniot.uflex.features.media.data.remote.dto.ConfirmMediaUploadRequestDto
import com.kiniot.uflex.features.media.data.remote.dto.CreateMediaUploadRequestDto
import com.kiniot.uflex.features.media.data.remote.dto.MediaAssetDto
import com.kiniot.uflex.features.media.data.remote.dto.MediaUploadTicketDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Backend endpoints of the media bounded context. These calls go through the
 * authenticated OkHttp client (Bearer added by AuthInterceptor). The actual file
 * bytes are NOT sent here — they go straight to Supabase via [SupabaseMediaUploader].
 */
interface MediaApiService {

    @POST("media/uploads")
    suspend fun createUpload(@Body request: CreateMediaUploadRequestDto): Response<MediaUploadTicketDto>

    @POST("media/uploads/{id}/confirm")
    suspend fun confirmUpload(
        @Path("id") mediaAssetId: String,
        @Body request: ConfirmMediaUploadRequestDto,
    ): Response<MediaAssetDto>

    @GET("media")
    suspend fun listByOwner(
        @Query("ownerType") ownerType: String,
        @Query("ownerId") ownerId: String? = null,
    ): Response<List<MediaAssetDto>>

    @GET("media/{id}")
    suspend fun getById(@Path("id") mediaAssetId: String): Response<MediaAssetDto>

    @DELETE("media/{id}")
    suspend fun delete(@Path("id") mediaAssetId: String): Response<Unit>
}
