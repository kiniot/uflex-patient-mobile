package com.kiniot.uflex.features.device.domain.usecase

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.device.domain.repository.DeviceConnectionRepository
import com.kiniot.uflex.features.device.domain.repository.DeviceRepository
import javax.inject.Inject

/**
 * Resolves the patient's assigned kit from the backend and opens the BLE link to it,
 * confirming the kit serial. This is the high-level entry point that ties the identity
 * bridge (GET devices/my-assigned) to the on-device connection.
 */
class ConnectToAssignedDeviceUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val connectionRepository: DeviceConnectionRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return when (val assigned = deviceRepository.getMyAssignedDevice()) {
            is AppResult.Success -> connectionRepository.connect(
                advertisedName = assigned.data.advertisedName,
                expectedSerial = assigned.data.serialNumber
            )
            is AppResult.Error -> assigned
        }
    }
}
