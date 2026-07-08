package com.kiniot.uflex.features.plan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseResponseDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val bodyPart: String? = null,
    val movementType: String? = null,
    val videoAssetId: String? = null,
    val videoUrl: String? = null
)
