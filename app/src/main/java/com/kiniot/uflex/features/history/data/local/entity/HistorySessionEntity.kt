package com.kiniot.uflex.features.history.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_sessions")
data class HistorySessionEntity(
    @PrimaryKey val sessionId: String,
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
    val finalizedAt: String?,
    val savedAt: String
)
