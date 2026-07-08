package com.kiniot.uflex.features.therapy.domain.model

data class SessionSummary(
    val sessionId: String,
    val patientId: String?,
    val totalSeries: Int,
    val completedSeries: Int,
    val totalRepetitions: Int,
    val goodRepetitions: Int,
    val incompleteRepetitions: Int,
    val unsafeRepetitions: Int,
    val averageAchievedRom: Double?,
    val painLevel: Int?,
    val requiresClinicalReview: Boolean,
    val compensatoryMovementsDetected: Int,
    val startedAt: String?,
    val finalizedAt: String?
)
