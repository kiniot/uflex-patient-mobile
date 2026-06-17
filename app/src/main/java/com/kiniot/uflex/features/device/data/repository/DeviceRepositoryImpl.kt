package com.kiniot.uflex.features.device.data.repository

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.device.data.mapper.toDomain
import com.kiniot.uflex.features.device.data.remote.datasource.DeviceRemoteDataSource
import com.kiniot.uflex.features.device.domain.model.Device
import com.kiniot.uflex.features.device.domain.repository.DeviceRepository
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val remoteDataSource: DeviceRemoteDataSource
) : DeviceRepository {
    override suspend fun getMyAssignedDevice(): AppResult<Device> {
        return when (val response = remoteDataSource.getMyAssignedDevice()) {
            is AppResult.Success -> AppResult.Success(response.data.toDomain())
            is AppResult.Error -> response
        }
    }
}
