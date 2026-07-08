package com.kiniot.uflex.features.history.domain.usecase

import com.kiniot.uflex.features.history.domain.model.HistorySession
import com.kiniot.uflex.features.history.domain.repository.HistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    operator fun invoke(): Flow<List<HistorySession>> = repository.observeCompletedSessions()
}

class SaveCompletedSessionHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    suspend operator fun invoke(session: HistorySession) {
        repository.saveCompletedSession(session)
    }
}
