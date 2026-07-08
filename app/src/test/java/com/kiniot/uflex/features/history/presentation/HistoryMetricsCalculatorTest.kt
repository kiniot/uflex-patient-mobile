package com.kiniot.uflex.features.history.presentation

import com.kiniot.uflex.features.history.domain.model.HistorySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryMetricsCalculatorTest {
    @Test
    fun correctRepPercentageIsNullWhenThereAreNoRepetitions() {
        val metrics = calculateHistoryMetrics(
            listOf(historySession(totalRepetitions = 0, goodRepetitions = 0))
        )

        assertNull(metrics.correctRepPercentage)
    }

    @Test
    fun correctRepPercentageUsesGoodRepetitionsOverTotalRepetitions() {
        val metrics = calculateHistoryMetrics(
            listOf(
                historySession(totalRepetitions = 10, goodRepetitions = 7),
                historySession(totalRepetitions = 5, goodRepetitions = 2)
            )
        )

        assertEquals(60, metrics.correctRepPercentage)
    }

    @Test
    fun improvementIsNullWhenThereAreFewerThanTwoRomPoints() {
        assertNull(calculateHistoryMetrics(emptyList()).improvementDegrees)
        assertNull(calculateHistoryMetrics(listOf(historySession(averageAchievedRom = 80.0))).improvementDegrees)
    }

    @Test
    fun improvementUsesChronologicalFirstAndLastRom() {
        val metrics = calculateHistoryMetrics(
            listOf(
                historySession(sessionId = "latest", averageAchievedRom = 92.4, finalizedAt = "2026-07-03T10:00:00Z"),
                historySession(sessionId = "first", averageAchievedRom = 80.2, finalizedAt = "2026-07-01T10:00:00Z"),
                historySession(sessionId = "middle", averageAchievedRom = 86.0, finalizedAt = "2026-07-02T10:00:00Z")
            )
        )

        assertEquals(12, metrics.improvementDegrees)
        assertEquals(listOf("first", "middle", "latest"), metrics.romPoints.map { it.sessionId })
    }

    @Test
    fun sessionsAreOrderedNewestFirstByFinalizedDate() {
        val ordered = orderedHistorySessions(
            listOf(
                historySession(sessionId = "old", finalizedAt = "2026-07-01T10:00:00Z"),
                historySession(sessionId = "new", finalizedAt = "2026-07-03T10:00:00Z"),
                historySession(sessionId = "middle", finalizedAt = "2026-07-02T10:00:00Z")
            )
        )

        assertEquals(listOf("new", "middle", "old"), ordered.map { it.sessionId })
    }

    private fun historySession(
        sessionId: String = "session",
        totalRepetitions: Int = 0,
        goodRepetitions: Int = 0,
        averageAchievedRom: Double? = null,
        finalizedAt: String? = "2026-07-01T10:00:00Z"
    ): HistorySession = HistorySession(
        sessionId = sessionId,
        patientId = "patient",
        totalSeries = 1,
        completedSeries = 1,
        totalRepetitions = totalRepetitions,
        goodRepetitions = goodRepetitions,
        incompleteRepetitions = 0,
        unsafeRepetitions = 0,
        averageAchievedRom = averageAchievedRom,
        painLevel = null,
        requiresClinicalReview = false,
        compensatoryMovementsDetected = 0,
        startedAt = "2026-07-01T09:00:00Z",
        finalizedAt = finalizedAt,
        savedAt = "2026-07-01T10:01:00Z"
    )
}
