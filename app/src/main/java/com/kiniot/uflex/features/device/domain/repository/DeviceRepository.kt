package com.kiniot.uflex.features.device.domain.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.device.domain.model.Device

interface DeviceRepository {
    suspend fun getMyAssignedDevice(): AppResult<Device>
}
