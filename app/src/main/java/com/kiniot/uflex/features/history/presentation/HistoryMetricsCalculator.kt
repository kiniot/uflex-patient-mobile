package com.kiniot.uflex.features.history.presentation

import com.kiniot.uflex.features.history.domain.model.HistorySession
import kotlin.math.roundToInt

data class HistoryMetrics(
    val correctRepPercentage: Int? = null,
    val improvementDegrees: Int? = null,
    val romPoints: List<RomHistoryPoint> = emptyList()
)

data class RomHistoryPoint(
    val sessionId: String,
    val label: String,
    val value: Float
)

fun orderedHistorySessions(sessions: List<HistorySession>): List<HistorySession> =
    sessions.sortedWith(
        compareByDescending<HistorySession> { it.finalizedAt ?: it.savedAt }
            .thenByDescending { it.savedAt }
    )

fun calculateHistoryMetrics(sessions: List<HistorySession>): HistoryMetrics {
    val totalRepetitions = sessions.sumOf { it.totalRepetitions }
    val correctRepPercentage = if (totalRepetitions > 0) {
        ((sessions.sumOf { it.goodRepetitions }.toFloat() / totalRepetitions) * 100).roundToInt()
    } else {
        null
    }

    val romPoints = orderedHistorySessions(sessions)
        .asReversed()
        .mapNotNull { session ->
            session.averageAchievedRom?.let { session.sessionId to it.toFloat() }
        }
        .mapIndexed { index, (sessionId, value) ->
            RomHistoryPoint(
                sessionId = sessionId,
                label = (index + 1).toString(),
                value = value
            )
        }

    val improvementDegrees = if (romPoints.size >= 2) {
        (romPoints.last().value - romPoints.first().value).roundToInt()
    } else {
        null
    }

    return HistoryMetrics(
        correctRepPercentage = correctRepPercentage,
        improvementDegrees = improvementDegrees,
        romPoints = romPoints
    )
}
