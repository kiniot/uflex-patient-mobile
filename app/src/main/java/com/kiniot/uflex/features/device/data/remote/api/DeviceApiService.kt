package com.kiniot.uflex.features.device.data.remote.api

import com.kiniot.uflex.features.device.data.remote.dto.DeviceResponseDto
import retrofit2.Response
import retrofit2.http.GET

private const val DEVICES_ENDPOINT_PATH = "devices"

interface DeviceApiService {
    @GET("$DEVICES_ENDPOINT_PATH/my-assigned")
    suspend fun getMyAssignedDevice(): Response<DeviceResponseDto>
}
