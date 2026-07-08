package com.kiniot.uflex.features.device.data.ble

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.model.MotionTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Low-level BLE access to the uFlex kit, expressed purely in app/domain terms so the rest of
 * the feature stays independent of the concrete BLE library. The implementation owns the
 * connection lifecycle and must be a singleton so the link survives screen recreation.
 */
interface DeviceBleDataSource {

    /** Current connection lifecycle state. */
    val connectionState: StateFlow<BleConnectionState>

    /**
     * Scans for the kit by its advertised name (filtered by the uFlex service UUID), connects,
     * and confirms its identity by reading the serial characteristic and checking it equals
     * [expectedSerial]. On success the link is ready and telemetry can be observed.
     */
    suspend fun connect(advertisedName: String, expectedSerial: String): AppResult<Unit>

    /**
     * Stream of decoded telemetry frames while connected. Cold: collecting subscribes to the
     * notify characteristic; cancelling the collection stops notifications.
     */
    fun observeTelemetry(): Flow<MotionTelemetry>

    /** Tears down the active connection, if any. */
    suspend fun disconnect()
}
