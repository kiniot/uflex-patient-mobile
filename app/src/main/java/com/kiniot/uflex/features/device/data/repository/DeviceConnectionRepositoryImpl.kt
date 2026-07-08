package com.kiniot.uflex.features.device.data.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.device.data.ble.DeviceBleDataSource
import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.model.MotionTelemetry
import com.kiniot.uflex.features.device.domain.repository.DeviceConnectionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class DeviceConnectionRepositoryImpl @Inject constructor(
    private val bleDataSource: DeviceBleDataSource
) : DeviceConnectionRepository {

    override val connectionState: StateFlow<BleConnectionState>
        get() = bleDataSource.connectionState

    override suspend fun connect(advertisedName: String, expectedSerial: String): AppResult<Unit> {
        return bleDataSource.connect(advertisedName, expectedSerial)
    }

    override fun observeTelemetry(): Flow<MotionTelemetry> {
        return bleDataSource.observeTelemetry()
    }

    override suspend fun disconnect() {
        bleDataSource.disconnect()
    }
}
