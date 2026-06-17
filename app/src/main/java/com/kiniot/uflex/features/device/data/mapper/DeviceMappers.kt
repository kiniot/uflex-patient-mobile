package com.kiniot.uflex.features.device.data.mapper

import com.kiniot.uflex.features.device.data.remote.dto.DeviceResponseDto
import com.kiniot.uflex.features.device.domain.model.Device
import com.kiniot.uflex.features.device.domain.model.DeviceCalibrationStatus
import com.kiniot.uflex.features.device.domain.model.DeviceStatus

fun DeviceResponseDto.toDomain(): Device {
    return Device(
        id = id,
        serialNumber = serialNumber,
        macAddress = macAddress,
        firmwareVersion = firmwareVersion,
        batteryLevel = batteryLevel,
        model = model,
        advertisedName = advertisedName,
        calibrationStatus = calibrationStatus.toDeviceCalibrationStatus(),
        status = status.toDeviceStatus(),
        lastSeenAt = lastSeenAt,
        tenantId = clinicId,
        currentPatientId = currentPatientId,
        offline = offline
    )
}

private fun String.toDeviceCalibrationStatus(): DeviceCalibrationStatus {
    return when (this) {
        "VALID" -> DeviceCalibrationStatus.Valid
        "NEEDS_CALIBRATION" -> DeviceCalibrationStatus.NeedsCalibration
        else -> DeviceCalibrationStatus.Unknown
    }
}

private fun String.toDeviceStatus(): DeviceStatus {
    return when (this) {
        "AVAILABLE" -> DeviceStatus.Available
        "ASSIGNED" -> DeviceStatus.Assigned
        "IN_MAINTENANCE" -> DeviceStatus.InMaintenance
        "RETIRED" -> DeviceStatus.Retired
        else -> DeviceStatus.Unknown
    }
}
