package com.kiniot.uflex.features.therapy.domain.model

/** Lifecycle status of a serie within a session. Mirrors the backend SerieStatus enum. */
enum class SerieStatus {
    Pending,
    Started,
    Completed,
    Failed,
    Unknown
}

/** Per-serie execution progress (rep counts + status). */
data class SerieProgress(
    val serieId: String,
    val exerciseId: String?,
    val currentRepetitions: Int,
    val targetRepetitions: Int,
    val status: SerieStatus
)

/**
 * Live execution progress of a session, read authoritatively from the backend
 * (`GET .../progress`). The rep counts advance as the edge forwards repetitions.
 */
data class SessionProgress(
    val sessionId: String,
    val status: SessionStatus,
    val currentSerieId: String?,
    val completedSeries: Int,
    val totalSeries: Int,
    val painLevel: Int?,
    val requiresClinicalReview: Boolean,
    val series: List<SerieProgress>
)
