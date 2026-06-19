package com.kiniot.uflex.features.media.data.repository

import com.kiniot.uflex.features.media.data.remote.SupabaseMediaUploader
import com.kiniot.uflex.features.media.data.remote.api.MediaApiService
import com.kiniot.uflex.features.media.data.remote.dto.ConfirmMediaUploadRequestDto
import com.kiniot.uflex.features.media.data.remote.dto.CreateMediaUploadRequestDto
import com.kiniot.uflex.features.media.data.remote.dto.MediaAssetDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the signed-URL upload flow for the patient mobile app:
 *
 *   1. ask the backend for a signed upload ticket,
 *   2. PUT the bytes directly to Supabase Storage,
 *   3. confirm the upload so the asset becomes UPLOADED.
 *
 * Read the bytes from the picked content Uri in the presentation/ViewModel layer
 * (e.g. `contentResolver.openInputStream(uri)?.readBytes()`) and pass them here.
 */
@Singleton
class MediaRepository @Inject constructor(
    private val api: MediaApiService,
    private val uploader: SupabaseMediaUploader,
) {

    suspend fun upload(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        ownerType: String,
        ownerId: String? = null,
    ): Result<MediaAssetDto> = runCatching {
        val mediaType = if (contentType.startsWith("video/")) "VIDEO" else "IMAGE"

        val ticketResponse = api.createUpload(
            CreateMediaUploadRequestDto(
                ownerType = ownerType,
                ownerId = ownerId,
                mediaType = mediaType,
                contentType = contentType,
                fileName = fileName,
                sizeBytes = bytes.size.toLong(),
            )
        )
        val ticket = ticketResponse.body()
            ?: error("Could not get an upload ticket (HTTP ${ticketResponse.code()})")

        uploader.upload(ticket.uploadUrl, bytes, contentType)

        val confirmResponse = api.confirmUpload(
            ticket.mediaAssetId,
            ConfirmMediaUploadRequestDto(sizeBytes = bytes.size.toLong()),
        )
        confirmResponse.body()
            ?: error("Could not confirm the upload (HTTP ${confirmResponse.code()})")
    }

    suspend fun listByOwner(ownerType: String, ownerId: String? = null): Result<List<MediaAssetDto>> =
        runCatching {
            val response = api.listByOwner(ownerType, ownerId)
            response.body() ?: error("Could not list media (HTTP ${response.code()})")
        }

    suspend fun delete(mediaAssetId: String): Result<Unit> = runCatching {
        val response = api.delete(mediaAssetId)
        if (!response.isSuccessful) error("Could not delete media (HTTP ${response.code()})")
    }
}
