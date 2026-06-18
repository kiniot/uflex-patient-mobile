package com.kiniot.uflex.features.media.data.remote.dto

import kotlinx.serialization.Serializable

/** Step 1 request body: POST /media/uploads */
@Serializable
data class CreateMediaUploadRequestDto(
    val ownerType: String,
    val ownerId: String? = null,
    val mediaType: String,
    val contentType: String,
    val fileName: String? = null,
    val sizeBytes: Long? = null,
)

/** Step 1 response: everything needed to upload directly to Supabase Storage. */
@Serializable
data class MediaUploadTicketDto(
    val mediaAssetId: String,
    val bucket: String,
    val objectPath: String,
    val uploadUrl: String,
    val token: String,
    val expiresInSeconds: Long,
    val status: String,
)

/** Step 3 request body: POST /media/uploads/{id}/confirm */
@Serializable
data class ConfirmMediaUploadRequestDto(
    val sizeBytes: Long? = null,
)

/** A stored media asset, with a short-lived signed download URL. */
@Serializable
data class MediaAssetDto(
    val id: String,
    val ownerType: String,
    val ownerId: String? = null,
    val mediaType: String,
    val status: String,
    val contentType: String,
    val originalFileName: String? = null,
    val sizeBytes: Long? = null,
    val downloadUrl: String? = null,
    val createdAt: String? = null,
)
