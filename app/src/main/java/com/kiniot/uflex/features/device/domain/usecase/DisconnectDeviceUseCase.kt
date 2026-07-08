package com.kiniot.uflex.features.device.domain.usecase

import com.kiniot.uflex.features.device.domain.repository.DeviceConnectionRepository
import javax.inject.Inject

/** Tears down the BLE link to the kit. */
class DisconnectDeviceUseCase @Inject constructor(
    private val connectionRepository: DeviceConnectionRepository
) {
    suspend operator fun invoke() {
        connectionRepository.disconnect()
    }
}
