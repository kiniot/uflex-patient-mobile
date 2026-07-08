package com.kiniot.uflex.features.therapy.domain.model

/** Lifecycle status of a therapy session. Mirrors the backend SessionStatus enum. */
enum class SessionStatus {
    Pending,
    Ready,
    InProgress,
    Completed,
    Cancelled,
    Unknown
}

/** A therapy session, as the patient app needs it during preparation. */
data class TherapySession(
    val id: String,
    val status: SessionStatus,
    val sensorsPlaced: Boolean?
)
