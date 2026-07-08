package com.kiniot.uflex.features.therapy.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire shape of the edge SSE `rep` event (snake_case). */
@Serializable
data class LiveRepEventDto(
    @SerialName("serie_id") val serieId: String,
    @SerialName("reps_detected") val repsDetected: Int,
    @SerialName("classification") val classification: String? = null,
    @SerialName("recorded_at") val recordedAt: String? = null
)
