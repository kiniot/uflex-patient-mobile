package com.kiniot.uflex.features.device.domain.usecase

import com.kiniot.uflex.features.device.domain.model.BleConnectionState
import com.kiniot.uflex.features.device.domain.repository.DeviceConnectionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Exposes the current BLE connection lifecycle state to the presentation layer. */
class ObserveDeviceConnectionStateUseCase @Inject constructor(
    private val connectionRepository: DeviceConnectionRepository
) {
    operator fun invoke(): StateFlow<BleConnectionState> {
        return connectionRepository.connectionState
    }
}
