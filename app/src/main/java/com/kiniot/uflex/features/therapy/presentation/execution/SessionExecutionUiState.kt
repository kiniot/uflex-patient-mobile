package com.kiniot.uflex.features.therapy.presentation.execution

import com.kiniot.uflex.core.ui.UiText
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.model.MotionTelemetry
import com.kiniot.uflex.features.therapy.domain.model.SerieProgress
import com.kiniot.uflex.features.therapy.domain.model.SerieStatus
import com.kiniot.uflex.features.therapy.domain.model.SessionProgress

/** UI state for the session-execution screen. */
data class SessionExecutionUiState(
    val phase: Phase = Phase.Loading,
    val progress: SessionProgress? = null,
    val connectionState: BleConnectionState = BleConnectionState.Idle,
    val latestTelemetry: MotionTelemetry? = null,
    val isStartingSerie: Boolean = false,
    val isFinalizing: Boolean = false,
    val painDialogVisible: Boolean = false,
    val errorMessage: UiText? = null
) {
    enum class Phase { Loading, Active, Finished, Failed }

    /** The serie currently being executed (Started), if any. */
    val runningSerie: SerieProgress?
        get() = progress?.series?.firstOrNull { it.status == SerieStatus.Started }

    /** The next serie waiting to be started, if any. */
    val nextPendingSerie: SerieProgress?
        get() = progress?.series?.firstOrNull { it.status == SerieStatus.Pending }

    /** True once every serie of the routine is completed (enables finalize). */
    val allSeriesCompleted: Boolean
        get() = progress?.let { it.totalSeries > 0 && it.completedSeries >= it.totalSeries } == true

    val connected: Boolean
        get() = connectionState is BleConnectionState.Connected
}
