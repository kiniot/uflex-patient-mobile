package com.kiniot.uflex.features.device.domain.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.model.MotionTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain-facing contract for the live BLE link to the kit. Separate from [DeviceRepository],
 * which handles the remote device registry; this one owns the on-device Bluetooth connection.
 */
interface DeviceConnectionRepository {

    val connectionState: StateFlow<BleConnectionState>

    /**
     * Connects to the kit identified by [advertisedName], confirming its [expectedSerial]
     * after connecting (see device-identity-contract).
     */
    suspend fun connect(advertisedName: String, expectedSerial: String): AppResult<Unit>

    fun observeTelemetry(): Flow<MotionTelemetry>

    suspend fun disconnect()
}
