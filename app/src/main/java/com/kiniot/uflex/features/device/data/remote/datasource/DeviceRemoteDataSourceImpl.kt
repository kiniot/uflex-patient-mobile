package com.kiniot.uflex.features.device.data.remote.datasource

import com.kiniot.uflex.core.network.SafeApiCaller
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.device.data.remote.api.DeviceApiService
import com.kiniot.uflex.features.device.data.remote.dto.DeviceResponseDto
import javax.inject.Inject

class DeviceRemoteDataSourceImpl @Inject constructor(
    private val apiService: DeviceApiService,
    private val safeApiCaller: SafeApiCaller
) : DeviceRemoteDataSource {
    override suspend fun getMyAssignedDevice(): AppResult<DeviceResponseDto> {
        return safeApiCaller.execute {
            apiService.getMyAssignedDevice()
        }
    }
}
