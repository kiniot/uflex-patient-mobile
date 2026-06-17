package com.kiniot.uflex.features.device.data.remote.datasource

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.device.data.remote.dto.DeviceResponseDto

interface DeviceRemoteDataSource {
    suspend fun getMyAssignedDevice(): AppResult<DeviceResponseDto>
}
