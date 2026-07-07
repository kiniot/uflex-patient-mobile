package com.kiniot.uflex.features.history.data.mapper

import com.kiniot.uflex.features.history.data.local.entity.HistorySessionEntity
import com.kiniot.uflex.features.history.domain.model.HistorySession

fun HistorySessionEntity.toDomain(): HistorySession = HistorySession(
    sessionId = sessionId,
    patientId = patientId,
    totalSeries = totalSeries,
    completedSeries = completedSeries,
    totalRepetitions = totalRepetitions,
    goodRepetitions = goodRepetitions,
    incompleteRepetitions = incompleteRepetitions,
    unsafeRepetitions = unsafeRepetitions,
    averageAchievedRom = averageAchievedRom,
    painLevel = painLevel,
    requiresClinicalReview = requiresClinicalReview,
    compensatoryMovementsDetected = compensatoryMovementsDetected,
    startedAt = startedAt,
    finalizedAt = finalizedAt,
    savedAt = savedAt
)

fun HistorySession.toEntity(): HistorySessionEntity = HistorySessionEntity(
    sessionId = sessionId,
    patientId = patientId,
    totalSeries = totalSeries,
    completedSeries = completedSeries,
    totalRepetitions = totalRepetitions,
    goodRepetitions = goodRepetitions,
    incompleteRepetitions = incompleteRepetitions,
    unsafeRepetitions = unsafeRepetitions,
    averageAchievedRom = averageAchievedRom,
    painLevel = painLevel,
    requiresClinicalReview = requiresClinicalReview,
    compensatoryMovementsDetected = compensatoryMovementsDetected,
    startedAt = startedAt,
    finalizedAt = finalizedAt,
    savedAt = savedAt
)
