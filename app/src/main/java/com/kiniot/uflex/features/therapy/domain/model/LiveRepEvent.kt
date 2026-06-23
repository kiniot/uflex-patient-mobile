package com.kiniot.uflex.features.therapy.domain.model

/**
 * A live, optimistic rep-count update pushed by the edge over SSE.
 *
 * [repsDetected] is the edge-local absolute tally for the active serie (resets on
 * serie change), so the UI takes the max and never moves backwards. The backend's
 * `GET .../progress` poll remains the authoritative source that reconciles it.
 */
data class LiveRepEvent(
    val serieId: String,
    val repsDetected: Int,
    val classification: String?
)
