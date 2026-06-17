package com.kiniot.uflex.features.device.domain.usecase

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.device.domain.model.Device
import com.kiniot.uflex.features.device.domain.repository.DeviceRepository
import javax.inject.Inject

class GetMyAssignedDeviceUseCase @Inject constructor(
    private val repository: DeviceRepository
) {
    suspend operator fun invoke(): AppResult<Device> {
        return repository.getMyAssignedDevice()
    }
}
